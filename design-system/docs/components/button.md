# Button · botón

> Esqueleto del contrato. Las tareas de cada pieza (PLATODO-18, PLATODO-20 y PLATODO-22) completan ejemplos y capturas; PLATODO-24 cierra la documentación final.

Contrato común de `Button` (`app`, `backoffice`) y `AppButton` (`movil`). Solo usa tokens de `design-system/tokens`.

## Uso

Acción de una pantalla. La etiqueta es un verbo concreto («Reservar cancha», «Guardar cambios»). Una acción crítica no se deshabilita sin explicar el motivo junto al botón.

## Propiedades

| Propiedad | Tipo | Regla |
|---|---|---|
| `variant` | `primary` · `secondary` · `danger` · `text` | Por defecto `primary` |
| `label` | texto | Verbo concreto; también es el nombre accesible |
| `loading` | booleano | Muestra spinner, no es clicable y anuncia el estado (`aria-busy`) |
| `disabled` | booleano | No es clicable ni enfocable como acción |

| Evento | Cuándo |
|---|---|
| `press` / `click` / `onPressed` (según la plataforma) | Al activar el botón; nunca con `loading` ni `disabled` |

## Estados

| Variante | Normal | Hover | Foco | Pulsado | Cargando | Deshabilitado |
|---|---|---|---|---|---|---|
| Primario | Fondo `color-primary`, texto `color-on-primary` | `color-primary-hover` | Anillo `color-focus` | `color-primary-hover` + sombra interior | Spinner + «Guardando…» | Fondo `color-disabled-bg`, texto `color-disabled-fg`, sin sombra |
| Secundario | Fondo `color-surface`, borde `color-primary` de `border-width-md`, texto `color-primary` | Fondo `color-primary-tint` | Anillo `color-focus` | [POR DEFINIR: tono del «primary-tint más oscuro»] | Spinner | Borde y texto `color-disabled-border` |
| Peligro | Fondo `color-danger`, texto `color-on-primary` | [POR DEFINIR: tono «más oscuro» del danger] | Anillo `color-focus` | — | Spinner | Como el primario deshabilitado |
| Texto | Texto `color-primary` | Subrayado | Anillo `color-focus` | — | — | Texto `color-disabled-fg` |

Medidas: alto mínimo `size-touch-min`; radio `radius-md`; etiqueta en `font-family-body`, `font-weight-semibold`.

## Accesibilidad

Foco visible con anillo de `border-focus-ring` y separación `border-focus-offset`; activable con Enter y Espacio; nombre accesible = `label`.

## React (`app`)

Ubicación: `app/src/shared/ui/button/Button.tsx` (exportado desde `app/src/shared/ui`). Estilos en `Button.module.css`, solo con `var(--…)`.

```tsx
import { Button } from '../shared/ui'

<Button label="Reservar cancha" onClick={reserve} />
<Button variant="secondary" label="Ver horarios" />
<Button variant="danger" label="Cancelar reserva" loading loadingLabel="Cancelando…" />
<Button label="Guardar cambios" disabled />
```

| Contrato | React |
|---|---|
| `variant` | `variant` (`primary` por defecto) |
| `label` | `label` (texto visible y nombre accesible) |
| `loading` | `loading`; texto `loadingLabel` (por defecto «Guardando…») |
| `disabled` | `disabled` (atributo nativo: fuera del orden de tabulación) |
| `press` | `onClick` |

Además acepta los atributos de `<button>` (`type` es `button` por defecto).

- **Cargando:** `aria-busy="true"` y `aria-disabled="true"`; sigue enfocable para no perder el foco del teclado, pero `onClick` no se llama. El spinner (Lucide `LoaderCircle`) no gira con `prefers-reduced-motion`.
- **Foco:** anillo `color-focus` de `border-focus-ring` separado `border-focus-offset`, con halo `color-focus-halo`, solo con `:focus-visible` (`app/src/shared/ui/a11y.module.css`).
- **Pulsado del primario:** `color-primary-hover` + sombra interior de `border-width-md` en `color-brand-navy`, provisional hasta definir la sombra exacta.
- **Pendientes ([POR DEFINIR]):** el pulsado del secundario usa el mismo `color-primary-tint` del hover y el hover de peligro mantiene `color-danger`, hasta que la identidad defina esos tonos.
- **Pruebas:** `Button.test.tsx` (activación con mouse, Enter y Espacio; deshabilitado; cargando; axe en cada variante × normal, cargando y deshabilitado).
- **Catálogo:** `pnpm dev` y abrir `/dev/design` (solo en desarrollo).

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al pulsar |
