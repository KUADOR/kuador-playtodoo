# Cambios a documentos compartidos · PLATODO-21

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## docs/architecture.md

En «Arquitectura del frontend», fila `backoffice`, añadir a los componentes de `src/app/shared/ui/`:

- `BookingCalendar` (`booking-calendar/`): calendario de reservas presentacional y siempre de solo lectura (decisión D6 de PLATODO-15). No llama a la `api`; recibe duración de franja y franjas, y emite `rangeChange` para que la pantalla cargue los datos. El modelo de presentación (`calendar-model.ts`, `calendar-dates.ts`) son funciones puras sin dependencias de Angular.

## AGENTS.md

Sin cambios.

## README.md

Sin cambios.
