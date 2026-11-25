# Button · botón

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

## Flutter (`movil`)

`movil/lib/shared/ui/app_button.dart`. Se apoya en `FilledButton` (primario y peligro), `OutlinedButton` (secundario) y `TextButton` (texto) con los estilos de `AppTheme.light()`, para conservar el ripple, la activación con teclado y la semántica nativos.

```dart
AppButton(label: 'Reservar cancha', onPressed: reserve);
AppButton(label: 'Ver horarios', variant: AppButtonVariant.secondary, leadingIcon: AppIcons.clock, onPressed: showSlots);
AppButton(label: 'Cancelar reserva', variant: AppButtonVariant.danger, isLoading: true, loadingLabel: 'Cancelando…', onPressed: cancel);
const AppButton(label: 'Guardar cambios', onPressed: null); // deshabilitado
```

| Contrato | Flutter |
|---|---|
| `variant` | `variant: AppButtonVariant` (`primary` por defecto) |
| `label` | `label` (texto visible y etiqueta de Semantics) |
| `loading` | `isLoading`; texto `loadingLabel` (por defecto «Guardando…») |
| `disabled` | `onPressed: null` (Semantics `enabled: false`, fuera del recorrido de foco) |
| `press` | `onPressed` |

- **Cargando:** el botón de Material queda deshabilitado (ignora toques y sale del recorrido de foco), pero conserva los colores de su variante, como en web; se muestra un `CircularProgressIndicator` con `loadingLabel` y el cambio se anuncia como región viva de Semantics. Con movimiento reducido (`MediaQuery.disableAnimations`) el indicador queda quieto como un arco de tres cuartos (`AppButton.reducedMotionSpinnerValue`), igual que el icono `LoaderCircle` de web.
- **Foco:** `AppFocusRing` (anillo `color-focus` de `border-focus-ring`, separado `border-focus-offset`, con halo `color-focus-halo`), solo con foco de teclado (`FocusHighlightMode.traditional`, equivalente a `:focus-visible`).
- **Tamaño táctil:** `minimumSize` de `size-touch-min` (44 × 44) y `MaterialTapTargetSize.padded`.
- **Pendientes ([POR DEFINIR]):** el pulsado del secundario usa `color-primary-tint` y el de peligro mantiene `color-danger` (más el ripple), hasta que la identidad defina esos tonos.
- **Pruebas:** `movil/test/shared/ui/app_button_test.dart` (cada variante: 44 × 44, `androidTapTargetGuideline`, `iOSTapTargetGuideline`, `labeledTapTargetGuideline`, `textContrastGuideline`; deshabilitado; cargando; anillo de foco).
- **Catálogo:** `flutter run` en `movil/` y «Ver catálogo de diseño» en la pantalla de inicio, o `flutter run --route /design-catalog` (solo en depuración). Muestra cada variante en normal, cargando y deshabilitado, como los catálogos web.

## Angular (`backoffice`)

Componente standalone `Button` en `backoffice/src/app/shared/ui/button/` (selector `app-button`), con iconos de `@lucide/angular`.

| Contrato | Angular |
|---|---|
| `variant`, `label`, `loading`, `disabled` | Entradas con el mismo nombre (`label` es obligatoria) |
| `press` | Salida `(press)`: solo se emite si no está `loading` ni `disabled` |
| — | `type` (`button` por defecto · `submit` · `reset`) para usarlo en formularios |
| — | `loadingLabel` (por defecto «Guardando…»): texto que reemplaza la etiqueta mientras carga |

```html
<app-button label="Guardar cambios" (press)="save()" />
<app-button variant="secondary" label="Ver complejo" />
<app-button variant="danger" label="Suspender complejo" [loading]="saving()" />
<app-button variant="text" label="Cancelar" [disabled]="true" />
```

- **Cargando:** spinner `LucideLoaderCircle` (oculto a lectores) + `loadingLabel`; el botón sigue enfocable, con `aria-busy="true"` y `aria-disabled="true"`, y si es `submit` no envía el formulario.
- **Deshabilitado:** atributo nativo `disabled` (sale del orden de tabulación).
- **Foco:** `:focus-visible` con anillo `color-focus` de `border-focus-ring` y separación `border-focus-offset`.
- **Pulsado del primario:** `color-primary-hover` + `inset var(--shadow-card)` como sombra interior.
- **Sin estilo propio mientras sigan [POR DEFINIR]:** hover del peligro y pulsado del secundario (este usa el mismo `color-primary-tint` del hover).
- El spinner respeta `prefers-reduced-motion`.
- Catálogo vivo: `npm start` en `backoffice/` y abrir `/design` (solo en desarrollo). Capturas en `docs/evidence/platodo-20/`.

## Diferencias aceptadas por plataforma

Lo que no está en esta tabla debe verse y comportarse igual en las tres piezas (mismos tokens, estados y textos). Comparación lado a lado: `docs/evidence/platodo-24/parity-button.png`.

| Plataforma | Diferencia | Motivo |
|---|---|---|
| `movil` | Ripple de Material al pulsar, en Android y en iOS (no se usa el resaltado de Cupertino) | Retroalimentación táctil nativa |
| `movil` | Sin hover: el estado hover solo aplica con puntero | Pantallas táctiles |
| `movil` | Cargando: el botón sale del recorrido de foco (en web sigue enfocable con `aria-busy` y `aria-disabled`) | Un botón de Material sin acción no recibe foco; el estado se anuncia como región viva |
| `movil` | Ancho completo dentro de su contenedor en el catálogo; en web el ancho se ajusta a la etiqueta | Convención de formularios móviles; el ancho lo decide la pantalla, no el componente |
| `app` · `backoffice` | Pulsado del primario: `app` usa sombra interior de `border-width-md` en `color-brand-navy` y `backoffice` usa `inset var(--shadow-card)` | Provisional mientras siga [POR DEFINIR: sombra exacta del pulsado]; al definirla se unifica con un token |

Pendientes comunes ([POR DEFINIR]): tono del pulsado del secundario y del hover de peligro.
