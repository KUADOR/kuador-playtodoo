# Cambios a documentos compartidos · PLATODO-21

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## docs/architecture.md

En «Arquitectura del frontend», fila `backoffice`, añadir a los componentes de `src/app/shared/ui/`:

- `BookingCalendar` (`booking-calendar/`): calendario de reservas presentacional y siempre de solo lectura (decisión D6 de PLATODO-15). No llama a la `api`; recibe duración de franja y franjas, y emite `rangeChange` para que la pantalla cargue los datos. El modelo de presentación (`calendar-model.ts`, `calendar-dates.ts`) son funciones puras sin dependencias de Angular.

## AGENTS.md

Sin cambios.

## README.md

Sin cambios.

## odd/tasks/platodo-15-identidad-visual.md (Seguimiento de PLATODO-15)

Decisión del Equipo Kuador (opción A, en PLATODO-21): ajustar el criterio 7 a «Dado el calendario de reservas, cuando se cambia entre vista diaria y semanal, entonces cada vista muestra los estados disponible, ocupado y seleccionado con el mismo aspecto; la vista mensual muestra por día un resumen de disponibilidad (`N libres` o `Sin horarios`) y lleva a la vista diaria». El contrato ya lo recoge en `design-system/docs/components/calendar.md` («Vista mensual»). Lo actualiza el Product Owner en el documento de la épica.
