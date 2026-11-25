# Cambios a documentos compartidos · PLATODO-35

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## DESIGN.md

Se implementaron en `app` dos de los componentes propuestos en Pantallas (PLATODO-25), pendientes de aprobación para sumarse a la Identidad visual:

- **Etiqueta de estado del personal** (componente 1): Libre, Pendiente (`color-warning` + reloj), Confirmada (`color-success` + check) y Bloqueo (candado + trama diagonal sobre `color-occupied-bg`). Siempre icono y texto.
- **Aviso y diálogo** (componente 3): `Alert` (error, advertencia, éxito, información; borde `border-width-md` del color de estado, `radius-lg`) y `Dialog` (`shadow-overlay`, foco atrapado, Esc, devuelve el foco y deja `inert` el resto de la página).

Pendientes de identidad:

- [POR DEFINIR: token del fondo (scrim) de los diálogos]. Hoy `color-mix` del navy al 40 %.

## docs/architecture.md

En «Clientes», fila `app`: primera funcionalidad con la organización propuesta, en inglés por la regla de idioma del código:

```
app/src/bookings/
  api/        (port StaffBookingsApi, HTTP adapter and contract mock)
  model/      (local time ↔ UTC, day schedule, texts)
  components/ (rows, forms and dialogs)
  screens/    (StaffCalendarPage)
```

## AGENTS.md

Sin cambios.

## README.md

Sin cambios.
