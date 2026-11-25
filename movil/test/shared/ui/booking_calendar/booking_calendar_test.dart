import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/shared/ui/booking_calendar/booking_calendar.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_slot_tile.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_text.dart';

import 'calendar_fixtures.dart';

const Size _phoneSize = Size(390, 844);

/// Pumps [child] in the app theme inside a vertical scroll view, on a phone
/// sized screen.
Future<void> _pumpCalendar(
  WidgetTester tester,
  Widget child, {
  bool disableAnimations = false,
}) {
  tester.view
    ..devicePixelRatio = 1
    ..physicalSize = _phoneSize;
  addTearDown(tester.view.reset);
  return tester.pumpWidget(
    MediaQuery(
      data: MediaQueryData(
        size: _phoneSize,
        disableAnimations: disableAnimations,
      ),
      child: MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTokens.space4),
            child: child,
          ),
        ),
      ),
    ),
  );
}

Finder _slotTile(String label) => find.byWidgetPredicate(
  (widget) => widget is CalendarSlotTile && widget.slot.label == label,
);

String? _focusedSlotStart() {
  final context = FocusManager.instance.primaryFocus?.context;
  final tile = context?.findAncestorWidgetOfExactType<CalendarSlotTile>();
  return tile?.slot.start;
}

/// Keeps the selection like a screen does: what the calendar reports comes
/// back as `selected`.
class _ControlledCalendar extends StatefulWidget {
  const _ControlledCalendar({required this.fixture, required this.onChange});

  final CalendarFixture fixture;
  final ValueChanged<SlotSelection?> onChange;

  @override
  State<_ControlledCalendar> createState() => _ControlledCalendarState();
}

class _ControlledCalendarState extends State<_ControlledCalendar> {
  late SlotSelection? _selected = widget.fixture.selected;

  @override
  Widget build(BuildContext context) {
    final fixture = widget.fixture;
    return BookingCalendar(
      slotDurationMinutes: fixture.slotDurationMinutes,
      days: fixture.days,
      today: fixture.today,
      initialView: fixture.view,
      mode: fixture.mode,
      selected: _selected,
      onSelectionChange: (selection) {
        widget.onChange(selection);
        setState(() => _selected = selection);
      },
    );
  }
}

void main() {
  group('day view against the shared fixtures', () {
    for (final name in ['day-30min', 'day-60min', 'day-90min']) {
      testWidgets('$name: labels, order, states and semantics', (tester) async {
        final handle = tester.ensureSemantics();
        final fixture = CalendarFixture.load(name);
        await _pumpCalendar(tester, calendarFrom(fixture));

        expect(find.text(fixture.expectedTitle), findsOneWidget);
        final tiles = tester
            .widgetList<CalendarSlotTile>(find.byType(CalendarSlotTile))
            .toList();
        final expectedSlots = fixture.expectedDaySlots;
        expect(
          [for (final tile in tiles) tile.slot.label],
          [for (final slot in expectedSlots) slot['label']],
        );
        for (final slot in expectedSlots) {
          final selectable = slot['selectable'] as bool;
          expect(
            tester.getSemantics(
              find.bySemanticsLabel(slot['accessibleLabel'] as String),
            ),
            isSemantics(
              label: slot['accessibleLabel'] as String,
              isSelected: slot['status'] == 'selected',
              hasEnabledState: true,
              isEnabled: selectable,
              isButton: selectable,
              hasTapAction: selectable,
            ),
          );
        }
        handle.dispose();
      });
    }

    testWidgets('three complexes with 30, 60 and 90 minutes at once', (
      tester,
    ) async {
      const keys = {
        'day-30min': ValueKey('calendar-30'),
        'day-60min': ValueKey('calendar-60'),
        'day-90min': ValueKey('calendar-90'),
      };
      await _pumpCalendar(
        tester,
        Column(
          children: [
            for (final MapEntry(key: name, value: key) in keys.entries)
              KeyedSubtree(
                key: key,
                child: calendarFrom(CalendarFixture.load(name)),
              ),
          ],
        ),
      );

      for (final MapEntry(key: name, value: key) in keys.entries) {
        final labels = tester
            .widgetList<CalendarSlotTile>(
              find.descendant(
                of: find.byKey(key),
                matching: find.byType(CalendarSlotTile, skipOffstage: false),
              ),
            )
            .map((tile) => tile.slot.label)
            .toList();
        expect(labels, [
          for (final slot in CalendarFixture.load(name).expectedDaySlots)
            slot['label'],
        ], reason: name);
      }
    });

    testWidgets('shows the slots in a grid of three columns', (tester) async {
      await _pumpCalendar(
        tester,
        calendarFrom(CalendarFixture.load('day-60min')),
      );

      final first = tester.getTopLeft(_slotTile('18:00–19:00'));
      final third = tester.getTopLeft(_slotTile('20:00–21:00'));
      final fourth = tester.getTopLeft(_slotTile('21:00–22:00'));
      expect(third.dy, first.dy);
      expect(fourth.dx, first.dx);
      expect(fourth.dy, greaterThan(first.dy));
    });

    testWidgets('occupied slots show lock and text, selected shows check', (
      tester,
    ) async {
      await _pumpCalendar(
        tester,
        calendarFrom(CalendarFixture.load('day-60min')),
      );

      expect(
        find.descendant(
          of: _slotTile('18:00–19:00'),
          matching: find.text('Ocupado'),
        ),
        findsOneWidget,
      );
      final selected = tester.widget<Ink>(
        find.descendant(
          of: _slotTile('19:00–20:00'),
          matching: find.byType(Ink),
        ),
      );
      expect(
        (selected.decoration! as BoxDecoration).color,
        AppTokens.colorPrimary,
      );
    });

    testWidgets('a day without slots shows the empty message', (tester) async {
      final fixture = CalendarFixture.load('day-empty');
      await _pumpCalendar(tester, calendarFrom(fixture));

      expect(find.text(CalendarText.emptyDay), findsOneWidget);
      expect(find.byType(CalendarSlotTile), findsNothing);
    });

    testWidgets('all occupied: summary «Sin horarios» and nothing selected', (
      tester,
    ) async {
      final handle = tester.ensureSemantics();
      await _pumpCalendar(
        tester,
        calendarFrom(CalendarFixture.load('day-all-occupied')),
      );

      expect(find.text(CalendarText.noSlots), findsOneWidget);
      expect(find.bySemanticsLabel(RegExp('Seleccionado')), findsNothing);
      handle.dispose();
    });

    testWidgets('every slot is at least 44 × 44', (tester) async {
      await _pumpCalendar(
        tester,
        calendarFrom(CalendarFixture.load('day-90min')),
      );

      for (final element in find.byType(CalendarSlotTile).evaluate()) {
        final size = tester.getSize(find.byWidget(element.widget));
        expect(size.width, greaterThanOrEqualTo(AppTokens.sizeTouchMin));
        expect(size.height, greaterThanOrEqualTo(AppTokens.sizeTouchMin));
      }
    });
  });

  // DESIGN.md sets the minimum touch area to 44 × 44, which is the iOS
  // guideline; Android's 48 × 48 is not the team rule.
  group('accessibility guidelines', () {
    for (final name in ['day-60min', 'week-mixed', 'month-mixed']) {
      testWidgets('$name meets touch target, label and contrast guidelines', (
        tester,
      ) async {
        final handle = tester.ensureSemantics();
        await _pumpCalendar(tester, calendarFrom(CalendarFixture.load(name)));

        await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        handle.dispose();
      });
    }
  });

  group('selection', () {
    testWidgets('an occupied slot cannot be selected', (tester) async {
      final events = <SlotSelection?>[];
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('day-60min'),
          onSelectionChange: events.add,
        ),
      );

      await tester.tap(_slotTile('18:00–19:00'));
      await tester.pump();

      expect(events, isEmpty);
    });

    testWidgets('a free slot is selected and activating it again clears it', (
      tester,
    ) async {
      final handle = tester.ensureSemantics();
      final events = <SlotSelection?>[];
      await _pumpCalendar(
        tester,
        _ControlledCalendar(
          fixture: CalendarFixture.load('day-60min'),
          onChange: events.add,
        ),
      );

      await tester.tap(_slotTile('20:00–21:00'));
      await tester.pump();
      expect(
        events.last,
        const SlotSelection(date: '2026-12-04', start: '20:00'),
      );
      expect(
        find.bySemanticsLabel('20:00 a 21:00, Seleccionado'),
        findsOneWidget,
      );
      expect(find.bySemanticsLabel('19:00 a 20:00, Libre'), findsOneWidget);

      await tester.tap(_slotTile('20:00–21:00'));
      await tester.pump();
      expect(events.last, isNull);
      expect(find.bySemanticsLabel(RegExp('Seleccionado')), findsNothing);
      handle.dispose();
    });

    testWidgets('a selection on an occupied slot is ignored', (tester) async {
      final handle = tester.ensureSemantics();
      final fixture = CalendarFixture.load('day-30min');
      await _pumpCalendar(tester, calendarFrom(fixture));

      expect(fixture.expectedSelected, isNull);
      expect(find.bySemanticsLabel(RegExp('Seleccionado')), findsNothing);
      expect(find.bySemanticsLabel('18:30 a 19:00, Ocupado'), findsOneWidget);
      handle.dispose();
    });
  });

  group('readonly', () {
    testWidgets('shows the notice, ignores the selection and emits nothing', (
      tester,
    ) async {
      final handle = tester.ensureSemantics();
      final events = <SlotSelection?>[];
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('week-readonly'),
          onSelectionChange: events.add,
        ),
      );

      expect(find.text(CalendarText.readonly), findsOneWidget);
      expect(find.bySemanticsLabel(RegExp('Seleccionado')), findsNothing);
      for (final element in _slotTile('19:00–20:00').evaluate().toList()) {
        await tester.ensureVisible(find.byWidget(element.widget));
        await tester.tap(find.byWidget(element.widget), warnIfMissed: false);
      }
      await tester.pump();

      expect(events, isEmpty);
      for (final tile in tester.widgetList<CalendarSlotTile>(
        find.byType(CalendarSlotTile, skipOffstage: false),
      )) {
        expect(tile.slot.selectable, isFalse);
      }
      expect(
        tester.getSemantics(
          find.bySemanticsLabel('18:00 a 19:00, Libre').first,
        ),
        isSemantics(
          label: '18:00 a 19:00, Libre',
          hasEnabledState: true,
          isEnabled: false,
          hasTapAction: false,
        ),
      );
      handle.dispose();
    });

    testWidgets('still navigates between periods', (tester) async {
      final ranges = <DateRange>[];
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('week-readonly'),
          onRangeChange: ranges.add,
        ),
      );

      await tester.tap(find.byTooltip('Semana anterior'));
      await tester.pump();

      expect(ranges, [const DateRange(from: '2026-11-23', to: '2026-11-29')]);
    });
  });

  group('week view', () {
    testWidgets('columns, rows and day context of each slot', (tester) async {
      final handle = tester.ensureSemantics();
      final fixture = CalendarFixture.load('week-mixed');
      await _pumpCalendar(tester, calendarFrom(fixture));

      expect(find.text(fixture.expectedTitle), findsOneWidget);
      expect(find.text(CalendarText.time), findsOneWidget);
      for (final header in ['lun 30', 'mié 2', 'dom 6']) {
        expect(find.text(header, skipOffstage: false), findsOneWidget);
      }
      expect(
        tester.getSemantics(
          find.bySemanticsLabel('19:00 a 20:00, Seleccionado'),
        ),
        isSemantics(
          label: '19:00 a 20:00, Seleccionado',
          hint: 'miércoles 2 de diciembre de 2026',
          isSelected: true,
          hasEnabledState: true,
          isEnabled: true,
          isButton: true,
          hasTapAction: true,
        ),
      );
      handle.dispose();
    });

    testWidgets('scrolls horizontally on a phone', (tester) async {
      await _pumpCalendar(
        tester,
        calendarFrom(CalendarFixture.load('week-mixed')),
      );

      final horizontal = find.byWidgetPredicate(
        (widget) =>
            widget is SingleChildScrollView &&
            widget.scrollDirection == Axis.horizontal,
      );
      expect(horizontal, findsOneWidget);
      final sunday = find.text('dom 6', skipOffstage: false);
      expect(tester.getTopLeft(sunday).dx, greaterThan(_phoneSize.width));

      await tester.drag(horizontal, const Offset(-600, 0));
      await tester.pump();
      expect(tester.getTopLeft(sunday).dx, lessThan(_phoneSize.width));
    });
  });

  group('month view', () {
    testWidgets('shows the summary per day and opens the chosen day', (
      tester,
    ) async {
      final ranges = <DateRange>[];
      final fixture = CalendarFixture.load('month-mixed');
      await _pumpCalendar(
        tester,
        calendarFrom(fixture, onRangeChange: ranges.add),
      );

      expect(find.text(fixture.expectedTitle), findsOneWidget);
      expect(find.text('1 libre'), findsOneWidget);
      expect(find.text('3 libres'), findsOneWidget);
      expect(find.text(CalendarText.today), findsOneWidget);

      await tester.tap(
        find.bySemanticsLabel(RegExp('^jueves 31 de diciembre')),
      );
      await tester.pump();

      expect(ranges, [const DateRange(from: '2026-12-31', to: '2026-12-31')]);
      expect(find.text('jueves 31 de diciembre de 2026'), findsOneWidget);
      expect(_slotTile('10:00–11:00'), findsOneWidget);
    });
  });

  group('navigation', () {
    testWidgets('next period and view change emit the new range', (
      tester,
    ) async {
      final ranges = <DateRange>[];
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('day-60min'),
          onRangeChange: ranges.add,
        ),
      );

      await tester.tap(find.byTooltip('Día siguiente'));
      await tester.pump();
      expect(find.text('sábado 5 de diciembre de 2026'), findsOneWidget);

      await tester.tap(find.text('Semana'));
      await tester.pumpAndSettle();

      expect(ranges, const [
        DateRange(from: '2026-12-05', to: '2026-12-05'),
        DateRange(from: '2026-11-30', to: '2026-12-06'),
      ]);
      expect(find.text('30 nov – 6 dic 2026'), findsOneWidget);
    });
  });

  group('loading and error', () {
    testWidgets('loading: skeleton and disabled navigation', (tester) async {
      final handle = tester.ensureSemantics();
      final ranges = <DateRange>[];
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('day-60min'),
          status: CalendarStatus.loading,
          onRangeChange: ranges.add,
        ),
      );

      expect(find.bySemanticsLabel(CalendarText.loading), findsOneWidget);
      expect(find.byType(CalendarSlotTile), findsNothing);
      await tester.tap(find.byTooltip('Día siguiente'));
      await tester.tap(find.text('Mes'));
      await tester.pump();
      expect(ranges, isEmpty);
      handle.dispose();
    });

    testWidgets('loading: the skeleton stays still with reduced motion', (
      tester,
    ) async {
      await _pumpCalendar(
        tester,
        calendarFrom(
          CalendarFixture.load('day-60min'),
          status: CalendarStatus.loading,
        ),
        disableAnimations: true,
      );
      await tester.pump(const Duration(milliseconds: 300));

      expect(tester.hasRunningAnimations, isFalse);
    });

    testWidgets('error: message and «Reintentar» emits retry', (tester) async {
      var retries = 0;
      final fixture = CalendarFixture.load('day-60min');
      await _pumpCalendar(
        tester,
        BookingCalendar(
          slotDurationMinutes: fixture.slotDurationMinutes,
          days: fixture.days,
          today: fixture.today,
          status: CalendarStatus.error,
          onRetry: () => retries++,
        ),
      );

      expect(find.text(CalendarText.loadError), findsOneWidget);
      await tester.tap(find.text(CalendarText.retry));
      expect(retries, 1);
    });
  });

  group('keyboard', () {
    testWidgets('arrows move between slots and Enter or Space select', (
      tester,
    ) async {
      final events = <SlotSelection?>[];
      await _pumpCalendar(
        tester,
        _ControlledCalendar(
          fixture: CalendarFixture.load('day-60min'),
          onChange: events.add,
        ),
      );

      for (var press = 0; press < 10 && _focusedSlotStart() == null; press++) {
        await tester.sendKeyEvent(LogicalKeyboardKey.tab);
        await tester.pump();
      }
      // The occupied 18:00 slot is not focusable: Tab lands on 19:00.
      expect(_focusedSlotStart(), '19:00');

      await tester.sendKeyEvent(LogicalKeyboardKey.arrowRight);
      await tester.pump();
      expect(_focusedSlotStart(), '20:00');

      await tester.sendKeyEvent(LogicalKeyboardKey.enter);
      await tester.pump();
      expect(
        events.last,
        const SlotSelection(date: '2026-12-04', start: '20:00'),
      );

      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      expect(_focusedSlotStart(), '21:00');

      await tester.sendKeyEvent(LogicalKeyboardKey.arrowUp);
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.space);
      await tester.pump();
      expect(_focusedSlotStart(), '20:00');
      expect(events.last, isNull);
    });
  });
}
