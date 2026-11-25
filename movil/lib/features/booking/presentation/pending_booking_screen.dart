import 'dart:async';

import 'package:flutter/semantics.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import '../../../shared/ui/app_button.dart';
import '../../../shared/ui/app_card.dart';
import '../../../shared/ui/app_notice.dart';
import '../domain/bookable_court.dart';
import '../domain/booking_failures.dart';
import '../domain/bookings_api.dart';
import '../domain/hold_countdown.dart';
import '../domain/player_booking.dart';
import 'booking_outcome_views.dart';
import 'booking_summary.dart';
import 'booking_text.dart';
import 'hold_countdown_banner.dart';

enum _Phase { holding, confirmed, expired }

/// Pantalla 3: summary of a pending booking with the 5-minute countdown.
/// It ends as «Reserva confirmada» or as «Tu tiempo terminó» (Pantalla 4).
///
/// The deadline is the server `expiresAt`; when the app comes back from the
/// background, the remaining time is recomputed and the effective status is
/// read again. Screen readers hear the start, 1:00, 0:30 and the end only.
class PendingBookingScreen extends StatefulWidget {
  const PendingBookingScreen({
    super.key,
    required this.api,
    required this.booking,
    required this.courtName,
    required this.timeZone,
    required this.receivedAt,
    required this.now,
  });

  final BookingsApi api;
  final PlayerBooking booking;
  final String courtName;
  final ComplexTimeZone timeZone;

  /// Device time when the booking response arrived, to estimate the clock
  /// difference with the server.
  final DateTime receivedAt;
  final DateTime Function() now;

  @override
  State<PendingBookingScreen> createState() => _PendingBookingScreenState();
}

class _PendingBookingScreenState extends State<PendingBookingScreen> {
  static const Duration _tickInterval = Duration(seconds: 1);

  late final HoldCountdown _countdown = HoldCountdown.fromServer(
    createdAt: widget.booking.createdAt,
    expiresAt: widget.booking.expiresAt ?? widget.booking.createdAt,
    receivedAt: widget.receivedAt,
  );
  late Duration _remaining = _countdown.remainingAt(widget.now());
  late final AppLifecycleListener _lifecycle;
  Timer? _ticker;
  _Phase _phase = _Phase.holding;
  bool _isConfirming = false;
  bool _isCancelling = false;
  ({String message, VoidCallback retry})? _actionError;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _onResume);
    if (_remaining == Duration.zero) {
      _phase = _Phase.expired;
      return;
    }
    _ticker = Timer.periodic(_tickInterval, (_) => _tick());
    WidgetsBinding.instance.addPostFrameCallback(
      (_) => _announce(formatHoldRemaining(_remaining)),
    );
  }

  @override
  void dispose() {
    _ticker?.cancel();
    _lifecycle.dispose();
    super.dispose();
  }

  void _tick() {
    if (_phase != _Phase.holding) return;
    final before = _remaining;
    final after = _countdown.remainingAt(widget.now());
    if (after == before) return;
    setState(() => _remaining = after);
    final milestone = HoldCountdown.milestoneBetween(before, after);
    if (milestone == HoldMilestone.expired) {
      _onHoldEnded();
    } else if (milestone != null) {
      _announce(holdMilestoneAnnouncement(milestone));
    }
  }

  /// The local countdown reached 0:00: the server says whether it was
  /// confirmed meanwhile; otherwise the hold is over.
  Future<void> _onHoldEnded() async {
    _ticker?.cancel();
    try {
      final booking = await widget.api.getBooking(widget.booking.id);
      _applyStatus(booking.status, holdEnded: true);
    } on BookingProblem {
      _showExpired();
    } on BookingConnectionFailure {
      _showExpired();
    }
  }

  Future<void> _onResume() async {
    if (_phase != _Phase.holding) return;
    _tick();
    try {
      final booking = await widget.api.getBooking(widget.booking.id);
      _applyStatus(booking.status, holdEnded: false);
    } on BookingProblem {
      _showExpired();
    } on BookingConnectionFailure {
      // Offline: the countdown goes on and the server decides on confirm.
    }
  }

  void _applyStatus(BookingStatus status, {required bool holdEnded}) {
    switch (status) {
      case BookingStatus.confirmed:
        _showConfirmed();
      case BookingStatus.expired || BookingStatus.cancelled:
        _showExpired();
      case BookingStatus.pending when holdEnded:
        _showExpired();
      case BookingStatus.pending:
        break;
    }
  }

  Future<void> _confirm() async {
    setState(() {
      _isConfirming = true;
      _actionError = null;
    });
    try {
      await widget.api.confirmBooking(widget.booking.id);
      _showConfirmed();
    } on BookingProblem {
      _showExpired();
    } on BookingConnectionFailure {
      _showActionError(BookingText.confirmFailed, _confirm);
    } finally {
      if (mounted) setState(() => _isConfirming = false);
    }
  }

  Future<void> _cancel() async {
    setState(() {
      _isCancelling = true;
      _actionError = null;
    });
    try {
      await widget.api.cancelBooking(widget.booking.id);
      _close();
    } on BookingProblem {
      // Already not pending: nothing is held any more.
      _close();
    } on BookingConnectionFailure {
      _showActionError(BookingText.cancelFailed, _cancel);
    } finally {
      if (mounted) setState(() => _isCancelling = false);
    }
  }

  void _showActionError(String message, VoidCallback retry) {
    if (!mounted || _phase != _Phase.holding) return;
    setState(() => _actionError = (message: message, retry: retry));
  }

  void _showConfirmed() {
    if (!mounted || _phase != _Phase.holding) return;
    _ticker?.cancel();
    setState(() => _phase = _Phase.confirmed);
    _announce(BookingText.confirmedTitle);
  }

  void _showExpired() {
    if (!mounted || _phase != _Phase.holding) return;
    _ticker?.cancel();
    setState(() => _phase = _Phase.expired);
    _announce(BookingText.expiredTitle, assertiveness: Assertiveness.assertive);
  }

  void _close() {
    if (mounted) Navigator.of(context).pop();
  }

  void _announce(
    String message, {
    Assertiveness assertiveness = Assertiveness.polite,
  }) {
    if (!mounted) return;
    SemanticsService.sendAnnouncement(
      View.of(context),
      message,
      Directionality.of(context),
      assertiveness: assertiveness,
    );
  }

  Widget _summary() => BookingSummary(
    courtName: widget.courtName,
    start: widget.booking.start,
    end: widget.booking.end,
    timeZone: widget.timeZone,
  );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: _phase == _Phase.holding
            ? const Text(BookingText.pendingTitle)
            : null,
      ),
      body: SafeArea(
        child: switch (_phase) {
          _Phase.holding => _buildHolding(context),
          _Phase.confirmed => BookingConfirmedView(
            summary: _summary(),
            onDone: _close,
          ),
          _Phase.expired => BookingExpiredView(onChooseSlot: _close),
        },
      ),
    );
  }

  Widget _buildHolding(BuildContext context) {
    final actionError = _actionError;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTokens.space4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                HoldCountdownBanner(
                  remaining: _remaining,
                  fractionLeft: _countdown.fractionLeft(_remaining),
                ),
                const SizedBox(height: AppTokens.space5),
                AppCard(child: _summary()),
                const SizedBox(height: AppTokens.space4),
                Text(
                  BookingText.holdHelp,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                if (actionError != null) ...[
                  const SizedBox(height: AppTokens.space4),
                  AppNotice(
                    tone: AppNoticeTone.error,
                    message: actionError.message,
                    action: AppButton(
                      label: BookingText.retry,
                      variant: AppButtonVariant.secondary,
                      onPressed: actionError.retry,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(AppTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AppButton(
                label: BookingText.confirm,
                loadingLabel: BookingText.confirming,
                isLoading: _isConfirming,
                onPressed: _isCancelling ? null : _confirm,
              ),
              const SizedBox(height: AppTokens.space2),
              AppButton(
                label: BookingText.cancel,
                loadingLabel: BookingText.cancelling,
                variant: AppButtonVariant.text,
                isLoading: _isCancelling,
                onPressed: _isConfirming ? null : _cancel,
              ),
            ],
          ),
        ),
      ],
    );
  }
}
