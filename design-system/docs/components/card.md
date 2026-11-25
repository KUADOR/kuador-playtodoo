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

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al tocar una tarjeta interactiva |
