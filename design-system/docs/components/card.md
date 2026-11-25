# Card · tarjeta

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

## React (`app`)

Ubicación: `app/src/shared/ui/card/Card.tsx` (exportado desde `app/src/shared/ui`). Estilos en `Card.module.css`, solo con `var(--…)`.

```tsx
import { Card } from '../shared/ui'

<Card variant="court">
  <h3>Cancha 1</h3>
  <p>Fútbol 5 · césped sintético</p>
</Card>
<Card variant="booking" selected>…</Card>
<Card variant="complex" loading />
```

| Contrato | React |
|---|---|
| `variant` | `variant` (se expone como `data-variant`; hoy todas las variantes comparten aspecto) |
| `selected` | `selected` |
| `loading` | `loading` |

Además acepta los atributos de `<div>`. El contenido va como `children`.

- **Seleccionada:** borde `color-primary` de `border-width-lg`, fondo `color-primary-tint` e icono Lucide `Check` sobre `color-primary`; el estado se anuncia con el texto oculto «Seleccionada». El relleno se compensa para que el contenido no se mueva al cambiar el borde.
- **Cargando:** reemplaza el contenido por un esqueleto en `color-disabled-bg`, con `aria-busy="true"` y el texto oculto «Cargando…». La animación de pulso se desactiva con `prefers-reduced-motion`.
- **Interacción:** la tarjeta es un contenedor. Si una pantalla la hace seleccionable, el control (botón o enlace) va dentro y lleva `aria-pressed` o `aria-selected` según el uso.
- **Pruebas:** `Card.test.tsx` (contenido, seleccionada, cargando; axe en cada variante × normal, seleccionada y cargando).
- **Catálogo:** `/dev/design`, sección «Tarjeta».

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
| `selected` | `selected`: borde `color-primary` de `border-width-lg`, fondo `color-primary-tint`, check `color-on-primary` sobre un círculo `color-primary` (relleno `space-1`, igual que en web) y Semantics `selected` |
| `loading` | `isLoading`: esqueleto en lugar de `child`; no responde a `onTap` |

- **Interactiva:** con `onTap` se comporta como botón (Semantics `button`, ripple, `AppFocusRing` y alto mínimo `size-touch-min`). `semanticLabel` reemplaza la semántica del contenido.
- **Movimiento reducido:** el esqueleto pulsa con opacidad y se queda quieto cuando `MediaQuery.disableAnimationsOf(context)` es verdadero («Quitar animaciones» en Android, «Reducir movimiento» en iOS).
- **Pruebas:** `movil/test/shared/ui/app_card_test.dart` (normal, seleccionada, esqueleto con y sin animación, toque y guías táctiles).

## Angular (`backoffice`)

Componente standalone `Card` en `backoffice/src/app/shared/ui/card/` (selector `app-card`). El contenido se proyecta con `<ng-content>`.

| Contrato | Angular |
|---|---|
| `variant` | Entrada obligatoria; se expone como `data-variant` en el elemento (hoy las cinco variantes se ven igual) |
| `selected`, `loading` | Entradas con el mismo nombre |

```html
<app-card variant="booking" [selected]="isSelected()">
  <p>Sábado 18:00 – 19:00</p>
  <p>Cancha 2 · confirmada</p>
</app-card>
<app-card variant="complex" [loading]="true" />
```

- **Seleccionada:** borde `color-primary` de `border-width-lg`, fondo `color-primary-tint` e icono `LucideCheck` con el texto oculto «Seleccionada» para lectores. La tarjeta no es interactiva por sí misma: si una pantalla la hace seleccionable, envuélvela en un botón o agrega el rol y `aria-pressed`/`aria-selected` según el uso.
- **Cargando:** el contenido se reemplaza por un esqueleto en `color-disabled-bg` con texto oculto «Cargando…» y `aria-busy="true"`; la animación se apaga con `prefers-reduced-motion`.
- Catálogo vivo en `/design` (solo desarrollo). Capturas en `docs/evidence/platodo-20/`.

## Diferencias aceptadas por plataforma

Lo que no está en esta tabla debe verse y comportarse igual en las tres piezas. Comparación lado a lado: `docs/evidence/platodo-24/parity-card.png`.

| Plataforma | Diferencia | Motivo |
|---|---|---|
| `movil` | Ripple de Material al tocar una tarjeta interactiva, en Android y en iOS | Retroalimentación táctil nativa |
| `movil` | `AppCard` puede ser interactiva (`onTap`); en web la tarjeta es un contenedor y el control va dentro | En móvil la tarjeta completa es el área táctil habitual |
| `movil` | La variante no es una propiedad de `AppCard`: la resuelve cada tarjeta de dominio | Hoy las cinco variantes se ven igual en las tres piezas |
| `movil` | El check va alineado al contenido (en una fila); en web va fijo en la esquina superior derecha | Mismo tamaño y colores; cambia solo la posición por el flujo de Flutter |
