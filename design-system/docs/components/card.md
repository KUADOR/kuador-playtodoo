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

## Flutter (`movil`)

`movil/lib/shared/ui/app_card.dart`. Es la base de las tarjetas de dominio (complejo, cancha, reserva…), que la envuelven con su contenido.

```dart
AppCard(
  semanticLabel: 'Cancha 1, fútbol 5',
  selected: isSelected,
  onTap: toggle,
  child: Text('Cancha 1 · Fútbol 5', style: Theme.of(context).textTheme.titleMedium),
);
const AppCard(isLoading: true, child: SizedBox.shrink());
```

| Contrato | Flutter |
|---|---|
| `variant` | Se resuelve en cada tarjeta de dominio; `AppCard` es la base común |
| `selected` | `selected`: borde `color-primary` de `border-width-lg`, fondo `color-primary-tint`, icono de check y Semantics `selected` |
| `loading` | `isLoading`: esqueleto en lugar de `child`; no responde a `onTap` |

- **Interactiva:** con `onTap` se comporta como botón (Semantics `button`, ripple, `AppFocusRing` y alto mínimo `size-touch-min`). `semanticLabel` reemplaza la semántica del contenido.
- **Movimiento reducido:** el esqueleto pulsa con opacidad y se queda quieto cuando `MediaQuery.disableAnimationsOf(context)` es verdadero («Quitar animaciones» en Android, «Reducir movimiento» en iOS).
- **Pruebas:** `movil/test/shared/ui/app_card_test.dart` (normal, seleccionada, esqueleto con y sin animación, toque y guías táctiles).

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al tocar una tarjeta interactiva, en Android y en iOS |
