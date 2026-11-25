import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import 'app_button.dart';

/// Tone of a notice: it sets the border and icon color, and the icon, so the
/// tone is never told only with color.
enum AppNoticeTone { warning, error }

/// Inline notice (Pantallas · componente 3): icon, optional short title,
/// message and optional action, on white with a 1.5 px border of the tone
/// color. It is announced when it appears.
class AppNotice extends StatelessWidget {
  const AppNotice({
    super.key,
    required this.tone,
    required this.message,
    this.title,
    this.action,
  });

  final AppNoticeTone tone;
  final String message;
  final String? title;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final color = _toneColor(context, tone);
    return Semantics(
      liveRegion: true,
      container: true,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surface,
          borderRadius: BorderRadius.circular(AppTokens.radiusLg),
          border: Border.all(color: color, width: AppTokens.borderWidthMd),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _NoticeMessage(tone: tone, title: title, message: message),
              if (action != null) ...[
                const SizedBox(height: AppTokens.space3),
                action!,
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// One button of a notice sheet; choosing it closes the sheet with [value].
class AppNoticeAction<T> {
  const AppNoticeAction({
    required this.label,
    required this.value,
    this.variant = AppButtonVariant.primary,
  });

  final String label;
  final T value;
  final AppButtonVariant variant;
}

/// Notice as a bottom sheet with full-width stacked buttons (Pantallas 2 and
/// 5 on mobile). The route traps focus, Esc or a tap outside closes it with
/// null, and focus goes back to where it was.
Future<T?> showAppNoticeSheet<T>({
  required BuildContext context,
  required AppNoticeTone tone,
  required String title,
  required String message,
  required List<AppNoticeAction<T>> actions,
  Widget? details,
}) {
  return showModalBottomSheet<T>(
    context: context,
    useSafeArea: true,
    isScrollControlled: true,
    backgroundColor: Theme.of(context).colorScheme.surface,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(
        top: Radius.circular(AppTokens.radiusLg),
      ),
    ),
    builder: (sheetContext) => _NoticeSheet<T>(
      tone: tone,
      title: title,
      message: message,
      actions: actions,
      details: details,
    ),
  );
}

class _NoticeSheet<T> extends StatelessWidget {
  const _NoticeSheet({
    required this.tone,
    required this.title,
    required this.message,
    required this.actions,
    required this.details,
  });

  final AppNoticeTone tone;
  final String title;
  final String message;
  final List<AppNoticeAction<T>> actions;
  final Widget? details;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      scopesRoute: true,
      namesRoute: true,
      explicitChildNodes: true,
      label: title,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppTokens.space5),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            _NoticeMessage(tone: tone, title: title, message: message),
            if (details != null) ...[
              const SizedBox(height: AppTokens.space4),
              details!,
            ],
            const SizedBox(height: AppTokens.space5),
            for (final (index, action) in actions.indexed) ...[
              if (index > 0) const SizedBox(height: AppTokens.space3),
              AppButton(
                label: action.label,
                variant: action.variant,
                onPressed: () => Navigator.of(context).pop(action.value),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _NoticeMessage extends StatelessWidget {
  const _NoticeMessage({
    required this.tone,
    required this.title,
    required this.message,
  });

  final AppNoticeTone tone;
  final String? title;
  final String message;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final icon = switch (tone) {
      AppNoticeTone.warning => AppIcons.warning,
      AppNoticeTone.error => AppIcons.alert,
    };
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AppIcon(icon, color: _toneColor(context, tone)),
        const SizedBox(width: AppTokens.space3),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (title != null) ...[
                Text(title!, style: textTheme.titleMedium),
                const SizedBox(height: AppTokens.space1),
              ],
              Text(message, style: textTheme.bodyMedium),
            ],
          ),
        ),
      ],
    );
  }
}

Color _toneColor(BuildContext context, AppNoticeTone tone) {
  final colors = context.appColors;
  return switch (tone) {
    AppNoticeTone.warning => colors.warning,
    AppNoticeTone.error => colors.danger,
  };
}
