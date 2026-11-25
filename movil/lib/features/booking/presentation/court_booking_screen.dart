import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import '../../../shared/ui/app_button.dart';
import '../../../shared/ui/app_notice.dart';
import '../../../shared/ui/booking_calendar/booking_calendar.dart';
import '../../../shared/ui/booking_calendar/calendar_dates.dart';
import '../../../shared/ui/booking_calendar/calendar_text.dart';
import '../domain/bookable_court.dart';
import '../domain/booking_failures.dart';
import '../domain/bookings_api.dart';
import '../domain/player_booking.dart';
import 'booking_summary.dart';
import 'booking_text.dart';
import 'court_calendar_days.dart';
import 'pending_booking_screen.dart';

/// Pantalla 1: the player picks a free slot of [court] and holds it as a
/// pending booking. Handles the conflict notice (Pantalla 2) and the warning
/// for a second booking at the same time (Pantalla 5), then opens the
/// countdown (Pantalla 3).
class CourtBookingScreen extends StatefulWidget {
  const CourtBookingScreen({
    super.key,
    required this.api,
    required this.court,
    required this.timeZone,
    required this.today,
    required this.now,
  });

  final BookingsApi api;
  final BookableCourt court;
  final ComplexTimeZone timeZone;

  /// Current local date of the complex.
  final IsoDate today;

  /// Device clock, injected so the countdown can be tested.
  final DateTime Function() now;

  @override
  State<CourtBookingScreen> createState() => _CourtBookingScreenState();
}

class _CourtBookingScreenState extends State<CourtBookingScreen> {
  late DateRange _range = DateRange(from: widget.today, to: widget.today);
  CalendarStatus _calendarStatus = CalendarStatus.loading;
  List<CalendarDay> _days = const [];
  SlotSelection? _selection;
  bool _isSubmitting = false;
  String? _reserveError;

  /// Only the answer of the latest load is applied.
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _loadOccupancy();
  }

  Future<void> _loadOccupancy() async {
    final generation = ++_loadGeneration;
    final range = _range;
    setState(() => _calendarStatus = CalendarStatus.loading);
    final window = occupancyWindow(range, widget.timeZone);
    try {
      final occupied = await widget.api.getCourtOccupancy(
        courtId: widget.court.id,
        from: window.from,
        to: window.to,
      );
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _days = _buildDays(range, occupied);
        _calendarStatus = CalendarStatus.ready;
      });
    } on BookingProblem {
      _showLoadError(generation);
    } on BookingConnectionFailure {
      _showLoadError(generation);
    }
  }

  void _showLoadError(int generation) {
    if (!mounted || generation != _loadGeneration) return;
    setState(() => _calendarStatus = CalendarStatus.error);
  }

  List<CalendarDay> _buildDays(DateRange range, List<OccupiedRange> occupied) =>
      buildCourtCalendarDays(
        court: widget.court,
        timeZone: widget.timeZone,
        range: range,
        occupied: occupied,
      );

  void _changeRange(DateRange range) {
    _range = range;
    _loadOccupancy();
  }

  void _select(SlotSelection? selection) {
    setState(() {
      _selection = selection;
      _reserveError = null;
    });
  }

  Future<void> _reserve({bool acknowledgeOwnOverlap = false}) async {
    final selection = _selection;
    if (selection == null) return;
    setState(() {
      _isSubmitting = true;
      _reserveError = null;
    });
    try {
      final booking = await widget.api.createPendingBooking(
        courtId: widget.court.id,
        start: selectionStartUtc(selection, widget.timeZone),
        acknowledgeOwnOverlap: acknowledgeOwnOverlap,
      );
      final receivedAt = widget.now();
      if (!mounted) return;
      setState(() => _isSubmitting = false);
      await _openPendingBooking(booking, receivedAt);
    } on OwnOverlapProblem catch (problem) {
      _stopSubmitting();
      await _askToBookAgain(problem.existingBooking);
    } on BookingProblem catch (problem) {
      _stopSubmitting();
      if (problem.code == BookingProblemCode.slotUnavailable) {
        await _showSlotUnavailable();
      } else {
        _showReserveError(BookingText.reserveRejected);
      }
    } on BookingConnectionFailure {
      _stopSubmitting();
      _showReserveError(BookingText.reserveFailed);
    }
  }

  void _stopSubmitting() {
    if (mounted) setState(() => _isSubmitting = false);
  }

  void _showReserveError(String message) {
    if (mounted) setState(() => _reserveError = message);
  }

  /// The slot was taken first: the stale selection is cleared and the
  /// availability refreshed while the notice is open.
  Future<void> _showSlotUnavailable() async {
    if (!mounted) return;
    setState(() => _selection = null);
    _loadOccupancy();
    await showAppNoticeSheet<void>(
      context: context,
      tone: AppNoticeTone.error,
      title: BookingText.slotUnavailableTitle,
      message: BookingText.slotUnavailable,
      actions: const [
        AppNoticeAction(label: BookingText.chooseAnotherSlot, value: null),
      ],
    );
  }

  Future<void> _askToBookAgain(PlayerBooking existingBooking) async {
    if (!mounted) return;
    final shouldContinue = await showAppNoticeSheet<bool>(
      context: context,
      tone: AppNoticeTone.warning,
      title: BookingText.ownOverlapTitle,
      message: BookingText.ownOverlapBody,
      details: BookingSummary(
        start: existingBooking.start,
        end: existingBooking.end,
        timeZone: widget.timeZone,
      ),
      actions: const [
        AppNoticeAction(label: BookingText.continueBooking, value: true),
        AppNoticeAction(
          label: BookingText.goBack,
          value: false,
          variant: AppButtonVariant.secondary,
        ),
      ],
    );
    if (shouldContinue ?? false) {
      await _reserve(acknowledgeOwnOverlap: true);
    }
  }

  Future<void> _openPendingBooking(
    PlayerBooking booking,
    DateTime receivedAt,
  ) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => PendingBookingScreen(
          api: widget.api,
          booking: booking,
          courtName: widget.court.name,
          timeZone: widget.timeZone,
          receivedAt: receivedAt,
          now: widget.now,
        ),
      ),
    );
    if (!mounted) return;
    setState(() => _selection = null);
    _loadOccupancy();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.court.name)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppTokens.space4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              BookingCalendar(
                slotDurationMinutes: widget.court.slotDurationMinutes,
                days: _days,
                today: widget.today,
                selected: _selection,
                status: _calendarStatus,
                onSelectionChange: _isSubmitting ? null : _select,
                onRangeChange: _changeRange,
                onRetry: _loadOccupancy,
              ),
              if (_reserveError != null) ...[
                const SizedBox(height: AppTokens.space4),
                AppNotice(tone: AppNoticeTone.error, message: _reserveError!),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: _ReserveFooter(
        selectionText: _selectionText(),
        isSubmitting: _isSubmitting,
        onReserve: _canReserve ? _reserve : null,
      ),
    );
  }

  bool get _canReserve =>
      _selection != null && _calendarStatus == CalendarStatus.ready;

  /// `Cancha 1 · viernes 4 de diciembre de 2026 · 20:00–21:00`.
  String? _selectionText() {
    final selection = _selection;
    if (selection == null) return null;
    final end = addMinutes(selection.start, widget.court.slotDurationMinutes);
    return '${widget.court.name} · ${formatLongDate(selection.date)} · '
        '${formatSlotLabel(selection.start, end)}';
  }
}

/// Fixed footer: the chosen slot, or why the button is disabled, and
/// «Reservar cancha».
class _ReserveFooter extends StatelessWidget {
  const _ReserveFooter({
    required this.selectionText,
    required this.isSubmitting,
    required this.onReserve,
  });

  final String? selectionText;
  final bool isSubmitting;
  final VoidCallback? onReserve;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: scheme.surface,
        border: Border(top: BorderSide(color: scheme.outlineVariant)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.all(AppTokens.space4),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                selectionText ?? BookingText.chooseFreeSlot,
                style: selectionText == null
                    ? textTheme.bodySmall?.copyWith(
                        color: scheme.onSurfaceVariant,
                      )
                    : textTheme.labelMedium,
              ),
              const SizedBox(height: AppTokens.space3),
              AppButton(
                label: BookingText.reserve,
                loadingLabel: BookingText.reserving,
                isLoading: isSubmitting,
                onPressed: onReserve,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
