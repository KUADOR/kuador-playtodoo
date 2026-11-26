/**
 * Date and time as «Pantallas» shows them: `dd/mm/aaaa hh:mm`, in the browser time zone.
 * The display time zone per country is [POR DEFINIR: launch country] (contract «Dates»).
 */
const DATE_TIME_FORMAT = new Intl.DateTimeFormat('es', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

export function formatDateTime(isoInstant: string): string {
  const parts = Object.fromEntries(
    DATE_TIME_FORMAT.formatToParts(new Date(isoInstant)).map((part) => [part.type, part.value]),
  );
  return `${parts['day']}/${parts['month']}/${parts['year']} ${parts['hour']}:${parts['minute']}`;
}
