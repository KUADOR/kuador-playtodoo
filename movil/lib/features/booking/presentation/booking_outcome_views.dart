import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../../../shared/ui/app_button.dart';
import '../../../shared/ui/app_card.dart';
import 'booking_text.dart';

/// Pantalla 4: the hold ended and the slot was released.
class BookingExpiredView extends StatelessWidget {
  const BookingExpiredView({super.key, required this.onChooseSlot});

  /// «Elegir un horario»: back to the calendar.
  final VoidCallback onChooseSlot;

  @override
  Widget build(BuildContext context) {
    return _OutcomeLayout(
      icon: AppIcons.clock,
      iconColor: Theme.of(context).colorScheme.onSurfaceVariant,
      title: BookingText.expiredTitle,
      body: const Text(BookingText.expiredBody, textAlign: TextAlign.center),
      actionLabel: BookingText.chooseSlot,
      onAction: onChooseSlot,
    );
  }
}

/// The booking was confirmed within the hold.
class BookingConfirmedView extends StatelessWidget {
  const BookingConfirmedView({
    super.key,
    required this.summary,
    required this.onDone,
  });

  final Widget summary;
  final VoidCallback onDone;

  @override
  Widget build(BuildContext context) {
    return _OutcomeLayout(
      icon: AppIcons.circleCheck,
      iconColor: context.appColors.success,
      title: BookingText.confirmedTitle,
      body: AppCard(child: summary),
      actionLabel: BookingText.backToCalendar,
      onAction: onDone,
    );
  }
}

class _OutcomeLayout extends StatelessWidget {
  const _OutcomeLayout({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.body,
    required this.actionLabel,
    required this.onAction,
  });

  static const double _iconSize = AppTokens.space7;

  final AppIcons icon;
  final Color iconColor;
  final String title;
  final Widget body;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTokens.space5),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: AppTokens.space6),
                AppIcon(icon, size: _iconSize, color: iconColor),
                const SizedBox(height: AppTokens.space4),
                Semantics(
                  header: true,
                  child: Text(
                    title,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                ),
                const SizedBox(height: AppTokens.space4),
                body,
              ],
            ),
          ),
        ),
        SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.all(AppTokens.space4),
            child: AppButton(label: actionLabel, onPressed: onAction),
          ),
        ),
      ],
    );
  }
}
