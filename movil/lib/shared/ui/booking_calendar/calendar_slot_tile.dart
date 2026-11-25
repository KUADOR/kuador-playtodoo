import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../app_focus_ring.dart';
import 'calendar_model.dart';
import 'calendar_text.dart';
import 'calendar_types.dart';

typedef _SlotLook = ({Color background, Color foreground, BoxBorder? border});

/// One slot of any view. The state is told with icon and text, never only
/// with color; screen readers get [SlotModel.accessibleLabel] plus the
/// selected and enabled flags.
///
/// [onActivate] runs only when the slot is selectable. [semanticHint] adds
/// context the label lacks, such as the day in the week view.
class CalendarSlotTile extends StatelessWidget {
  const CalendarSlotTile({
    super.key,
    required this.slot,
    required this.onActivate,
    this.semanticHint,
  });

  final SlotModel slot;
  final ValueChanged<SlotModel> onActivate;
  final String? semanticHint;

  static final BorderRadius _radius = BorderRadius.circular(AppTokens.radiusMd);

  _SlotLook _look(BuildContext context) {
    final colors = context.appColors;
    final scheme = Theme.of(context).colorScheme;
    return switch (slot.status) {
      SlotStatus.available => (
        background: colors.accentTint,
        foreground: colors.brandNavy,
        border: Border.all(
          color: colors.accentStrong,
          width: AppTokens.borderWidthSm,
        ),
      ),
      SlotStatus.occupied => (
        background: colors.occupiedBackground,
        foreground: colors.occupiedForeground,
        border: null,
      ),
      SlotStatus.selected => (
        background: scheme.primary,
        foreground: scheme.onPrimary,
        border: Border.all(
          color: scheme.primary,
          width: AppTokens.borderWidthSm,
        ),
      ),
    };
  }

  @override
  Widget build(BuildContext context) {
    final look = _look(context);
    final onTap = slot.selectable ? () => onActivate(slot) : null;

    final tile = Material(
      type: MaterialType.transparency,
      child: Ink(
        decoration: BoxDecoration(
          color: look.background,
          border: look.border,
          borderRadius: _radius,
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: _radius,
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              minWidth: AppTokens.sizeTouchMin,
              minHeight: AppTokens.sizeTouchMin,
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppTokens.space2,
                vertical: AppTokens.space2,
              ),
              child: _SlotContent(slot: slot, color: look.foreground),
            ),
          ),
        ),
      ),
    );

    return Semantics(
      container: true,
      excludeSemantics: true,
      label: slot.accessibleLabel,
      hint: semanticHint,
      selected: slot.status == SlotStatus.selected,
      enabled: slot.selectable,
      button: slot.selectable,
      onTap: onTap,
      child: AppFocusRing(borderRadius: _radius, child: tile),
    );
  }
}

class _SlotContent extends StatelessWidget {
  const _SlotContent({required this.slot, required this.color});

  /// Icon of the visible state line; available slots have none.
  static const Map<SlotStatus, AppIcons> _stateIcons = {
    SlotStatus.occupied: AppIcons.lock,
    SlotStatus.selected: AppIcons.check,
  };

  final SlotModel slot;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final stateIcon = _stateIcons[slot.status];

    return Column(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          slot.label,
          softWrap: false,
          style: textTheme.labelMedium?.copyWith(color: color),
        ),
        if (stateIcon != null)
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppIcon(stateIcon, size: AppTokens.fontSizeSm, color: color),
              // Occupied adds the visible word; selected is check plus time
              // (calendar.md) and «Seleccionado» goes to screen readers.
              if (slot.status == SlotStatus.occupied) ...[
                const SizedBox(width: AppTokens.space1),
                Flexible(
                  child: Text(
                    slotStatusText[SlotStatus.occupied]!,
                    style: textTheme.bodySmall?.copyWith(color: color),
                  ),
                ),
              ],
            ],
          ),
      ],
    );
  }
}

/// Dot plus the text «Hoy».
class TodayMark extends StatelessWidget {
  const TodayMark({super.key});

  static const double _dotSize = AppTokens.space2;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primary;
    // Wraps the text under the dot in narrow month cells instead of
    // overflowing.
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: AppTokens.space1,
      children: [
        Container(
          width: _dotSize,
          height: _dotSize,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        Text(
          CalendarText.today,
          style: Theme.of(context).textTheme.labelSmall?.copyWith(color: color),
        ),
      ],
    );
  }
}
