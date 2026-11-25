/**
 * Reference model of the BookingCalendar contract (docs/components/calendar.md).
 * It only exists to prove that every fixture's `expected` block is consistent with its `input`;
 * the three real implementations live in app, backoffice and movil.
 */

const MINUTES_PER_HOUR = 60;
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;
const DAYS_PER_WEEK = 7;
const MONDAY_INDEX = 1;

const WEEKDAY_NAMES = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const WEEKDAY_SHORT_NAMES = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
const MONTH_NAMES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];
const MONTH_SHORT_NAMES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const STATUS_TEXT = { available: 'Libre', occupied: 'Ocupado', selected: 'Seleccionado' };
const NO_SLOTS_SUMMARY = 'Sin horarios';
const EMPTY_DAY_MESSAGE = 'Este día no tiene horarios. Prueba con otro día.';

const parseDate = (isoDate) => new Date(`${isoDate}T00:00:00Z`);
const formatDate = (date) => date.toISOString().slice(0, 10);
const addDays = (isoDate, amount) => {
  const date = parseDate(isoDate);
  date.setUTCDate(date.getUTCDate() + amount);
  return formatDate(date);
};
const pad = (number) => String(number).padStart(2, '0');

function toMinutes(time) {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

function toTime(totalMinutes) {
  const minutesInDay = totalMinutes % MINUTES_PER_DAY;
  return `${pad(Math.floor(minutesInDay / MINUTES_PER_HOUR))}:${pad(minutesInDay % MINUTES_PER_HOUR)}`;
}

function mondayOf(isoDate) {
  const weekday = parseDate(isoDate).getUTCDay();
  const daysSinceMonday = (weekday - MONDAY_INDEX + DAYS_PER_WEEK) % DAYS_PER_WEEK;
  return addDays(isoDate, -daysSinceMonday);
}

function monthBounds(isoDate) {
  const date = parseDate(isoDate);
  const first = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), 1));
  const last = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0));
  return { from: formatDate(first), to: formatDate(last) };
}

function visibleRange(view, today) {
  if (view === 'day') return { from: today, to: today };
  if (view === 'week') {
    const from = mondayOf(today);
    return { from, to: addDays(from, DAYS_PER_WEEK - 1) };
  }
  return monthBounds(today);
}

function datesBetween({ from, to }) {
  const dates = [];
  for (let date = from; date <= to; date = addDays(date, 1)) dates.push(date);
  return dates;
}

function slotsByDate(days) {
  return new Map(days.map((day) => [day.date, day.slots]));
}

/** A selection only counts when it points to an existing available slot. */
function effectiveSelection(input) {
  const { selected, days } = input;
  if (!selected) return null;
  const slots = slotsByDate(days).get(selected.date) ?? [];
  const slot = slots.find((candidate) => candidate.start === selected.start);
  return slot?.status === 'available' ? { date: selected.date, start: selected.start } : null;
}

function displayStatus(date, slot, selection) {
  const isSelected = selection?.date === date && selection.start === slot.start;
  return isSelected ? 'selected' : slot.status;
}

function describeSlot(date, slot, input, selection) {
  const end = toTime(toMinutes(slot.start) + input.slotDurationMinutes);
  const status = displayStatus(date, slot, selection);
  return {
    start: slot.start,
    end,
    label: `${slot.start}–${end}`,
    status,
    accessibleLabel: `${slot.start} a ${end}, ${STATUS_TEXT[status]}`,
    selectable: input.mode === 'editable' && status !== 'occupied',
  };
}

function availabilitySummary(availableCount) {
  if (availableCount === 0) return NO_SLOTS_SUMMARY;
  return availableCount === 1 ? '1 libre' : `${availableCount} libres`;
}

function describeDay(date, input, selection) {
  const slots = [...(slotsByDate(input.days).get(date) ?? [])]
    .sort((a, b) => toMinutes(a.start) - toMinutes(b.start))
    .map((slot) => describeSlot(date, slot, input, selection));
  const availableCount = slots.filter((slot) => slot.status !== 'occupied').length;
  return {
    date,
    isToday: date === input.today,
    availableCount,
    occupiedCount: slots.length - availableCount,
    summary: availabilitySummary(availableCount),
    emptyMessage: slots.length === 0 ? EMPTY_DAY_MESSAGE : null,
    slots,
  };
}

function dayTitle(isoDate) {
  const date = parseDate(isoDate);
  return `${WEEKDAY_NAMES[date.getUTCDay()]} ${date.getUTCDate()} de ${MONTH_NAMES[date.getUTCMonth()]} de ${date.getUTCFullYear()}`;
}

function shortDate(isoDate) {
  const date = parseDate(isoDate);
  return `${date.getUTCDate()} ${MONTH_SHORT_NAMES[date.getUTCMonth()]}`;
}

function weekTitle({ from, to }) {
  return `${shortDate(from)} – ${shortDate(to)} ${parseDate(to).getUTCFullYear()}`;
}

function monthTitle(isoDate) {
  const date = parseDate(isoDate);
  return `${MONTH_NAMES[date.getUTCMonth()]} ${date.getUTCFullYear()}`;
}

function columnHeader(isoDate) {
  const date = parseDate(isoDate);
  return `${WEEKDAY_SHORT_NAMES[date.getUTCDay()]} ${date.getUTCDate()}`;
}

function weekLayout(range, input, selection) {
  const days = datesBetween(range).map((date) => describeDay(date, input, selection));
  const rows = [...new Set(days.flatMap((day) => day.slots.map((slot) => slot.start)))].sort(
    (a, b) => toMinutes(a) - toMinutes(b),
  );
  return {
    columns: days.map((day) => ({ date: day.date, header: columnHeader(day.date), isToday: day.isToday, summary: day.summary })),
    rows,
    cells: rows.map((start) => days.map((day) => day.slots.find((slot) => slot.start === start)?.status ?? null)),
  };
}

function monthLayout(range, input, selection) {
  const gridStart = mondayOf(range.from);
  const gridEnd = addDays(mondayOf(range.to), DAYS_PER_WEEK - 1);
  const cells = datesBetween({ from: gridStart, to: gridEnd }).map((date) => {
    if (date < range.from || date > range.to) return null;
    const day = describeDay(date, input, selection);
    return {
      date,
      dayOfMonth: parseDate(date).getUTCDate(),
      isToday: day.isToday,
      availableCount: day.availableCount,
      occupiedCount: day.occupiedCount,
      summary: day.summary,
    };
  });
  const weeks = [];
  for (let index = 0; index < cells.length; index += DAYS_PER_WEEK) weeks.push(cells.slice(index, index + DAYS_PER_WEEK));
  return { weekdayHeaders: ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'], weeks };
}

/** Expected output for a fixture input, following the contract. */
export function expectedOutput(input) {
  const range = visibleRange(input.view, input.today);
  const selection = input.mode === 'readonly' ? null : effectiveSelection(input);
  const base = { range, selected: selection };
  if (input.view === 'day') {
    return { ...base, title: dayTitle(range.from), day: describeDay(range.from, input, selection) };
  }
  if (input.view === 'week') {
    return { ...base, title: weekTitle(range), week: weekLayout(range, input, selection) };
  }
  return { ...base, title: monthTitle(range.from), month: monthLayout(range, input, selection) };
}
