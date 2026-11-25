# Card · tarjeta

> Esqueleto del contrato. Las tareas de cada pieza (PLATODO-18, PLATODO-20 y PLATODO-22) completan ejemplos y capturas; PLATODO-24 cierra la documentación final.

Contrato común de `Card` (`app`, `backoffice`) y `AppCard` (`movil`). Solo usa tokens de `design-system/tokens`.

## Uso

Agrupa la información de un complejo, una cancha, una reserva, un torneo o una novedad.

## Propiedades

| Propiedad | Tipo | Regla |
|---|---|---|
| `variant` | `complex` · `court` · `booking` · `tournament` · `news` | Qué representa la tarjeta |
| `selected` | booleano | Marca la tarjeta elegida |
| `loading` | booleano | Muestra el esqueleto en lugar del contenido |

## Estados

| Estado | Aspecto |
|---|---|
| Normal | Fondo `color-surface`, radio `radius-lg`, sombra `shadow-card`, relleno `space-4`, borde `color-border` |
| Seleccionada | Borde `color-primary` de `border-width-lg` + fondo `color-primary-tint` + icono de check |
| Cargando | Esqueleto gris animado; sin animación con `prefers-reduced-motion` |

## Accesibilidad

La selección no depende solo del color: lleva icono de check y se anuncia (`aria-selected` o `aria-pressed` según el uso; `Semantics` en Flutter).

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al tocar una tarjeta interactiva |
