# BookingCalendar · calendario de reservas

Contrato común de las tres implementaciones: `app` (React), `backoffice` (Angular, solo lectura) y `movil` (Flutter). La apariencia sale de la Identidad visual (DESIGN.md v2) y solo de los tokens de `design-system/tokens`. Diseño de la solución: PLATODO-15, sección 5.

El calendario es **presentacional**: muestra lo que recibe y emite la selección. No llama a la `api`, no calcula disponibilidad ni aplica reglas de reserva (conflictos, duplicados y concurrencia son del módulo Reservas).

## Uso

- **`movil`** (jugadores): elegir día y franja para reservar. Vista inicial `day`.
- **`app`** (personal del complejo): consultar ocupación y elegir franja. Vista inicial `week`.
- **`backoffice`** (Kuador): consultar ocupación de otro complejo. Vista inicial `week`, siempre `readonly`.

## Entradas

| Propiedad | Tipo | Regla |
|---|---|---|
| `slotDurationMinutes` | entero > 0 | Duración de franja del complejo (configurable por complejo). Define la hora de fin de cada franja |
| `days` | `{ date: "YYYY-MM-DD", slots: { start: "HH:mm", status: "available" \| "occupied" }[] }[]` | Hora local del complejo, sin conversión de zona horaria. Un día que no viene o viene sin franjas no tiene horarios |
| `view` / `initialView` | `day` · `week` · `month` | Por defecto `day` en `movil`, `week` en `app` y `backoffice` |
| `mode` | `editable` · `readonly` | En `backoffice` siempre `readonly` |
| `selected` | `{ date, start }` o `null` | Una franja a la vez. Si apunta a una franja ocupada o inexistente se ignora (se muestra sin selección) |
| `today` | `"YYYY-MM-DD"` | Se inyecta; el componente nunca lee el reloj. El calendario abre en el período que contiene `today` |
| `status` | `loading` · `error` · `ready` | Esqueleto, error con «Reintentar» o contenido |

La zona horaria del complejo es [POR DEFINIR: país y zona horaria] y se resuelve fuera del componente.

## Salidas

| Evento | Cuándo |
|---|---|
| `selectionChange({ date, start } \| null)` | Al activar una franja disponible, o la seleccionada para quitarla. Nunca en ocupadas ni en `readonly` |
| `rangeChange({ from, to })` | Al cambiar de vista o de período, para que la pantalla cargue los datos |
| `retry()` | Al pulsar «Reintentar» en el estado de error |

## Reglas de presentación

Estas reglas producen la salida esperada (`expected`) de cada fixture; las tres implementaciones deben coincidir con ella.

- **Fin de franja:** `start + slotDurationMinutes`, en `HH:mm`; pasada la medianoche vuelve a `00:00` (p. ej. `22:30` + 90 → `00:00`).
- **Etiqueta visible:** `«inicio»–«fin»` con raya corta (U+2013), p. ej. `18:00–19:00`.
- **Etiqueta accesible:** `«inicio» a «fin», «estado»`, con estado `Libre`, `Ocupado` o `Seleccionado`, p. ej. `18:00 a 19:00, Libre`.
- **Orden:** franjas por hora de inicio, aunque lleguen desordenadas.
- **Estado mostrado:** `selected` si coincide con la selección efectiva; si no, el recibido.
- **Seleccionable:** solo en `editable` y si el estado no es `occupied`.
- **Resumen del día:** `N libres` (`1 libre` en singular; cuenta disponibles y la seleccionada) o `Sin horarios` si no queda ninguna libre.
- **Día sin franjas (vista diaria):** «Este día no tiene horarios. Prueba con otro día.»
- **Hoy:** `isToday` marca el día con punto y texto «Hoy».
- **Períodos (`range`):** `day` = el día; `week` = de lunes a domingo; `month` = del día 1 al último del mes.
- **Títulos:** `day` → `viernes 4 de diciembre de 2026`; `week` → `30 nov – 6 dic 2026`; `month` → `diciembre 2026`. Días y meses en español, sin depender del idioma del dispositivo.
- **Vista semanal:** columnas de lunes a domingo con cabecera `lun 30`; filas = todas las horas de inicio de la semana, ordenadas; celda vacía (`null`) si ese día no tiene esa franja.
- **Vista mensual:** cuadrícula de semanas de lunes a domingo; los días fuera del mes son `null`; cada día muestra su resumen. Al elegir un día se pasa a la vista diaria de ese día.

## Estados

### Franja (iguales en las tres vistas; nunca solo color)

| Estado | Fondo | Borde | Icono y texto | Comportamiento |
|---|---|---|---|---|
| Disponible | `color-accent-tint` | `color-accent-strong`, `border-width-sm` | Hora en `color-brand-navy` (+ «Libre» para lectores) | Seleccionable |
| Ocupado | `color-occupied-bg` | ninguno | Candado + «Ocupado»; texto `color-fg-muted` | No seleccionable, `aria-disabled` |
| Seleccionado | `color-primary` | `color-primary` | Check + hora en `color-on-primary` | Una a la vez; activarla de nuevo la quita |
| Foco | el del estado | Anillo `color-focus` de `border-focus-ring` con separación `border-focus-offset` y halo `color-focus-halo` | — | Flechas entre franjas; Enter o Espacio selecciona |

### Componente

| Estado | Qué se ve |
|---|---|
| Cargando (`status: loading`) | Esqueleto; respeta `prefers-reduced-motion`. Cambiar vista y período queda deshabilitado |
| Error (`status: error`) | «No pudimos cargar los horarios.» + botón «Reintentar» (emite `retry`) |
| Vacío | «Este día no tiene horarios. Prueba con otro día.» |
| Solo lectura | Mismo aspecto sin acción ni hover; aviso fijo «Solo lectura» con icono de ojo |

## Accesibilidad

- Teclado: roving tabindex (una sola franja en el orden de tabulación); flechas entre franjas; Enter o Espacio selecciona. En Flutter, `Shortcuts` + `FocusTraversalGroup`.
- Lectores: etiqueta accesible de cada franja; `aria-selected` en la seleccionada y `aria-disabled` en las ocupadas (en Flutter, `Semantics`).
- Área mínima de cada franja: `size-touch-min` × `size-touch-min` (44 × 44).
- Contraste: los pares usados están en `design-system/contrast-pairs.json` y se validan con `npm test`.

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Vista diaria por defecto; franjas en rejilla de 3 columnas; vista semanal con desplazamiento horizontal; ripple de Material |
| `app` | Vista semanal por defecto |
| `backoffice` | Siempre `readonly` (decisión D6): no tiene `mode`, `selected` ni `selectionChange`; las franjas no son interactivas (sin hover, foco ni acción) y se muestra el aviso fijo «Solo lectura». Semana y mes se desplazan en horizontal en pantallas angostas |

## Fixtures compartidos

En `design-system/fixtures/calendar/` y copiados por `npm run build` a:

- `app/src/shared/ui/booking-calendar/fixtures/`
- `backoffice/src/app/shared/ui/booking-calendar/fixtures/`
- `movil/test/shared/ui/booking_calendar/fixtures/`

Cada archivo tiene `name`, `description`, `input` (las entradas del contrato) y `expected` (la salida). Las pruebas de cada pieza montan el componente con `input` y comprueban `expected`.

| Fixture | Qué cubre |
|---|---|
| `day-30min` | Mismo día con franjas de 30 min; la selección apunta a una ocupada y se ignora; franjas desordenadas en la entrada |
| `day-60min` | Mismo día con franjas de 60 min y una selección válida |
| `day-90min` | Mismo día con franjas de 90 min; la última termina a medianoche (`22:30–00:00`) |
| `day-empty` | Día sin franjas: mensaje de vacío |
| `day-all-occupied` | Todas ocupadas: nada seleccionable, resumen «Sin horarios» y selección ignorada |
| `week-mixed` | Semana con horas distintas por día, un día vacío, un día ausente en la entrada y una selección válida |
| `week-readonly` | Los mismos datos de `week-mixed` en `readonly` (para `backoffice`): nada seleccionable |
| `month-mixed` | Diciembre de 2026 (empieza en martes): días con libres, totalmente ocupados, vacíos y sin datos |

Forma de `expected`:

```json
{
  "range": { "from": "2026-12-04", "to": "2026-12-04" },
  "selected": { "date": "2026-12-04", "start": "19:00" },
  "title": "viernes 4 de diciembre de 2026",
  "day": {
    "date": "2026-12-04", "isToday": true, "availableCount": 3, "occupiedCount": 1,
    "summary": "3 libres", "emptyMessage": null,
    "slots": [
      { "start": "19:00", "end": "20:00", "label": "19:00–20:00", "status": "selected",
        "accessibleLabel": "19:00 a 20:00, Seleccionado", "selectable": true }
    ]
  }
}
```

En `week` la clave `day` se reemplaza por `week: { columns, rows, cells }` y en `month` por `month: { weekdayHeaders, weeks }`. Los tres casos de 30, 60 y 90 min muestran el mismo día de complejos distintos: es la prueba a nivel de componente de que la duración de franja de cada complejo se respeta.

## Angular (`backoffice`)

Componente standalone `BookingCalendar` en `backoffice/src/app/shared/ui/booking-calendar/` (selector `app-booking-calendar`), con iconos de `@lucide/angular` (ojo, candado, alerta y chevrons). **Diferencia aceptada:** el backoffice siempre es de solo lectura (decisión D6 de PLATODO-15: Kuador consulta otros complejos y no edita), así que no existe el modo editable.

| Contrato | Angular |
|---|---|
| `slotDurationMinutes`, `today` | Entradas obligatorias |
| `days`, `status` | Entradas (`[]` y `ready` por defecto) |
| `initialView` | Entrada, `week` por defecto |
| `mode`, `selected` | No existen: siempre `readonly`, sin selección. Si los datos traen una franja «seleccionada», se muestra con su estado recibido |
| `selectionChange` | No existe |
| `rangeChange` | Salida `(rangeChange)`: al cambiar de vista, de período o al abrir un día desde la semana o el mes. No se emite al montar ni si el período no cambia |
| `retry` | Salida `(retry)` desde «Reintentar» |

```html
<app-booking-calendar
  [slotDurationMinutes]="complex.slotDurationMinutes"
  [today]="today"
  [days]="days()"
  [status]="status()"
  (rangeChange)="loadSlots($event)"
  (retry)="reload()"
/>
```

- **Franjas:** mismos colores y textos que en las otras piezas (disponible y ocupado con candado y «Ocupado»), sin cursor, hover ni foco. El lector de pantalla lee la etiqueta accesible del contrato (`18:00 a 19:00, Libre`).
- **Navegación con teclado:** todos los controles son botones nativos (Tab, Enter, Espacio). Botones «Día/Semana/Mes anterior» y «… siguiente», selector de vista con `aria-pressed`, cabeceras de la semana que abren el día y cuadrícula del mes con roving tabindex: una sola fecha en el orden de tabulación (hoy o el día 1) y flechas para moverse dentro del mes. El título del período se anuncia (`aria-live`).
- **Estados:** cargando (esqueleto, `aria-busy`, navegación y vistas deshabilitadas, respeta `prefers-reduced-motion`), error («No pudimos cargar los horarios.» con `role="alert"` + «Reintentar») y vacío del día.
- **Móvil:** semana y mes dentro de una región desplazable en horizontal y enfocable; vista diaria en rejilla que se adapta al ancho.
- **Pruebas:** `booking-calendar.spec.ts` y `calendar-model.spec.ts` comprueban los fixtures (30, 60 y 90 min en paralelo, semana, mes, vacío, todo ocupado), que ninguna franja sea interactiva ni seleccionada, teclado, `rangeChange`, estados y axe.
- Catálogo vivo: `npm start` en `backoffice/` y abrir `/design`, sección «Calendario de reservas (solo lectura)». Capturas en `docs/evidence/platodo-21/`.

## React (`app`)

Ubicación: `app/src/shared/ui/booking-calendar/` (exportado desde `app/src/shared/ui`). Estilos en `BookingCalendar.module.css`, solo con `var(--…)`. Las reglas de presentación viven en funciones puras (`calendarModel.ts`, `calendarText.ts`, `calendarDates.ts`) que producen la forma de `expected`; el componente solo las pinta.

```tsx
import { BookingCalendar } from '../shared/ui'

<BookingCalendar
  slotDurationMinutes={60}
  days={days}
  today="2026-12-02"
  selected={selected}
  status={status}
  onSelectionChange={setSelected}
  onRangeChange={loadDays}
  onRetry={reload}
/>
```

| Contrato | React |
|---|---|
| `slotDurationMinutes`, `days`, `today`, `selected`, `mode`, `status` | Mismo nombre (`mode` por defecto `editable`, `status` por defecto `ready`, `selected` controlado por la pantalla) |
| `view` / `initialView` | `initialView` (por defecto `week`); luego la vista la cambia la persona |
| `selectionChange` · `rangeChange` · `retry` | `onSelectionChange` · `onRangeChange` · `onRetry` |

- **Vistas:** `day` es un `listbox` de selección única (`option` con `aria-selected` y `aria-disabled` en ocupadas); `week` y `month` son `grid` (tablas con `gridcell`). En `readonly`, `aria-readonly="true"`.
- **Teclado:** roving tabindex en las tres vistas: una sola celda en el orden de tabulación (la seleccionada, si no la de hoy, si no la primera). Flechas izquierda y derecha en orden de lectura, arriba y abajo por la misma columna (saltan los días sin esa franja), Inicio y Fin. Enter o Espacio seleccionan; en el mes abren la vista diaria de ese día.
- **Navegación:** botones «Día/Semana/Mes anterior» y «… siguiente» y el selector de vista (`aria-pressed`); se deshabilitan con `status: loading`. En `readonly` se puede navegar (emite `rangeChange` para cargar datos), pero activar franjas no emite nada.
- **Contenedor:** `role="group"` con el título como nombre (no es un landmark, para que una pantalla pueda mostrar varios calendarios).
- **Pantallas angostas:** las vistas semanal y mensual se desplazan horizontalmente cuando no caben.

## Flutter (`movil`)

Ubicación: `movil/lib/shared/ui/booking_calendar/`. Las reglas de presentación viven en funciones puras (`calendar_model.dart`, `calendar_text.dart`, `calendar_dates.dart`) que producen la forma de `expected`; los widgets solo las pintan. Solo tokens vía `AppTokens`, `ColorScheme` y `context.appColors`.

```dart
BookingCalendar(
  slotDurationMinutes: 60,
  days: days,
  today: '2026-12-02',
  selected: selected,
  status: status,
  onSelectionChange: (value) => setState(() => selected = value),
  onRangeChange: loadDays,
  onRetry: reload,
)
```

| Contrato | Flutter |
|---|---|
| `slotDurationMinutes`, `days`, `today`, `selected`, `mode`, `status` | Mismo nombre (`days`: `List<CalendarDay>`; estado de entrada `SlotAvailability`) |
| `view` / `initialView` | `initialView` (por defecto `CalendarView.day`) |
| `selectionChange` · `rangeChange` · `retry` | `onSelectionChange` · `onRangeChange` · `onRetry` |

- **Vistas:** `day` en rejilla de 3 columnas; `week` en tabla con desplazamiento horizontal (cada franja lleva el día como `hint` para lectores); `month` en cuadrícula que se desplaza si siete columnas no caben.
- **Franjas:** `Semantics` con la etiqueta accesible, `selected`, `enabled` y `button` solo si es seleccionable. Ocupada: candado + «Ocupado»; seleccionada: hora + check.
- **Teclado:** `Shortcuts` (flechas → `DirectionalFocusIntent`, Enter/Espacio → `ActivateIntent`) + `FocusTraversalGroup`. Las franjas no seleccionables no reciben foco (los lectores sí las leen). No hay roving tabindex: Tab recorre las franjas seleccionables.
- **Navegación:** botones de icono con `tooltip` «Día/Semana/Mes anterior/siguiente» y `SegmentedButton` para la vista; deshabilitados con `status: loading`.
- **Movimiento:** el esqueleto (`AppSkeleton`) queda quieto con `MediaQuery.disableAnimations`.
- **Área táctil:** 44 × 44 (`size-touch-min`), validado con `iOSTapTargetGuideline`.
