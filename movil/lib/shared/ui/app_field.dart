import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import 'app_focus_ring.dart';

/// One option of [AppField.select].
@immutable
class AppSelectOption<T> {
  const AppSelectOption({required this.value, required this.label});

  final T value;
  final String label;
}

/// Form field with a visible label above it, optional help text and an error
/// state (danger border, alert icon and message), all exposed to Semantics.
class AppField<T> extends StatelessWidget {
  const AppField.text({
    super.key,
    required this.label,
    this.controller,
    this.helperText,
    this.errorText,
    this.enabled = true,
    this.keyboardType,
    ValueChanged<String>? onChanged,
  }) : _kind = _FieldKind.text,
       onTextChanged = onChanged,
       options = const [],
       value = null,
       onSelected = null;

  const AppField.select({
    super.key,
    required this.label,
    required this.options,
    this.value,
    ValueChanged<T?>? onChanged,
    this.helperText,
    this.errorText,
    this.enabled = true,
  }) : _kind = _FieldKind.select,
       onSelected = onChanged,
       controller = null,
       keyboardType = null,
       onTextChanged = null;

  final _FieldKind _kind;
  final String label;
  final String? helperText;
  final String? errorText;
  final bool enabled;

  final TextEditingController? controller;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onTextChanged;

  final List<AppSelectOption<T>> options;
  final T? value;
  final ValueChanged<T?>? onSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // Help and error are read as part of the field node, so their visible
    // copies are excluded to avoid reading them twice.
    final decoration = InputDecoration(
      helper: helperText == null
          ? null
          : ExcludeSemantics(
              child: Text(
                helperText!,
                style: theme.inputDecorationTheme.helperStyle,
              ),
            ),
      error: errorText == null
          ? null
          : ExcludeSemantics(child: _FieldError(message: errorText!)),
    );
    final semanticHint = [errorText, helperText].nonNulls.join('. ');

    final Widget input = switch (_kind) {
      _FieldKind.text => TextField(
        controller: controller,
        enabled: enabled,
        keyboardType: keyboardType,
        onChanged: onTextChanged,
        decoration: decoration,
      ),
      _FieldKind.select => DropdownButtonFormField<T>(
        initialValue: value,
        isExpanded: true,
        icon: const AppIcon(AppIcons.chevronDown),
        decoration: decoration,
        onChanged: enabled ? onSelected : null,
        items: [
          for (final option in options)
            DropdownMenuItem<T>(value: option.value, child: Text(option.label)),
        ],
      ),
    };

    return MergeSemantics(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: theme.textTheme.labelLarge),
          const SizedBox(height: AppTokens.space1),
          Semantics(
            hint: semanticHint.isEmpty ? null : semanticHint,
            child: AppFocusRing(
              borderRadius: BorderRadius.circular(AppTokens.radiusMd),
              child: input,
            ),
          ),
        ],
      ),
    );
  }
}

enum _FieldKind { text, select }

class _FieldError extends StatelessWidget {
  const _FieldError({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final danger = context.appColors.danger;
    final style = Theme.of(context).inputDecorationTheme.errorStyle;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AppIcon(AppIcons.alert, color: danger, size: AppTokens.fontSizeMd),
        const SizedBox(width: AppTokens.space1),
        Expanded(child: Text(message, style: style)),
      ],
    );
  }
}
