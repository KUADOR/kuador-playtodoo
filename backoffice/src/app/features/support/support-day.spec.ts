import {
  AUDIT_ENTRIES,
  COURTS,
  dayEntries,
  localInstantToday,
} from '../../../testing/support-fixtures';
import { todayAsCalendarDate } from '../../core/format/date-format';
import { minutesLeft, sessionPhase } from './support-clock';
import { StaffBooking } from './support-bookings-api';
import { courtDay, courtName, dayRange, entryRows } from './support-day';
import { auditAction, minutesLeftText } from './support-texts';

describe('support day model (P9)', () => {
  const today = todayAsCalendarDate();

  it('asks the api for the whole local day', () => {
    const range = dayRange(today);

    expect(new Date(range.from).getHours()).toBe(0);
    expect(new Date(range.to).getTime() - new Date(range.from).getTime()).toBeGreaterThanOrEqual(
      23 * 3_600_000,
    );
    expect(todayAsCalendarDate(new Date(range.from))).toBe(today);
  });

  it('lists active entries by start, with court, kind, status and holder or reason', () => {
    const cancelled: StaffBooking = { ...dayEntries()[0], id: 'gone', status: 'CANCELLED' };

    const rows = entryRows([...dayEntries(), cancelled], COURTS);

    expect(
      rows.map((row) => [row.time, row.court, row.kind, row.statusLook?.label, row.detail]),
    ).toEqual([
      ['13:00–14:00', 'Cancha 2', 'Bloqueo', undefined, 'Maintenance'],
      ['19:00–20:00', 'Cancha 1', 'Reserva', 'Pendiente', null],
      ['20:00–21:00', 'Cancha 1', 'Reserva', 'Confirmada', 'Alex Example'],
    ]);
  });

  it('names a court the courts source does not know by the start of its id', () => {
    expect(courtName('abcdef12-0000-4000-8000-000000000000', COURTS)).toBe('Cancha abcdef12');
  });

  it('offers the slots of a court with the occupied ones marked and none in the past', () => {
    const tomorrow = todayAsCalendarDate(new Date(Date.now() + 86_400_000));
    const entries = [
      {
        ...dayEntries()[0],
        start: localInstantToday('19:00', 1),
        end: localInstantToday('20:00', 1),
      },
    ];

    expect(courtDay(COURTS[0], tomorrow, entries).slots).toEqual([
      { start: '18:00', status: 'available' },
      { start: '19:00', status: 'occupied' },
      { start: '20:00', status: 'available' },
      { start: '21:00', status: 'available' },
    ]);
    const lateToday = new Date();
    lateToday.setHours(20, 30, 0, 0);
    expect(courtDay(COURTS[0], today, [], lateToday).slots.map((slot) => slot.start)).toEqual([
      '21:00',
    ]);
  });
});

describe('support session countdown', () => {
  it('warns with 5 minutes or less and expires at 0', () => {
    expect(sessionPhase(5 * 60_000 + 1)).toBe('active');
    expect(sessionPhase(5 * 60_000)).toBe('expiring');
    expect(sessionPhase(0)).toBe('expired');
    expect(minutesLeftText(minutesLeft(29 * 60_000 + 1))).toBe('quedan 30 min');
    expect(minutesLeftText(minutesLeft(30_000))).toBe('queda menos de 1 min');
  });
});

describe('audit action (P7)', () => {
  it('names known operations in Spanish and shows the technical detail of the rest', () => {
    expect(AUDIT_ENTRIES.map(auditAction)).toEqual([
      { label: 'Canceló una reserva o quitó un bloqueo', technicalDetail: null },
      { label: 'Consultó reservas', technicalDetail: null },
      { label: 'Otra operación', technicalDetail: 'GET /api/v1/staff/reports' },
    ]);
  });
});
