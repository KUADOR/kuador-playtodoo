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

- **Cargando:** `onPressed` pasa a null, se muestra un `CircularProgressIndicator` con `loadingLabel` y el cambio se anuncia como región viva de Semantics.
- **Foco:** `AppFocusRing` (anillo `color-focus` de `border-focus-ring`, separado `border-focus-offset`, con halo `color-focus-halo`), solo con foco de teclado (`FocusHighlightMode.traditional`, equivalente a `:focus-visible`).
- **Tamaño táctil:** `minimumSize` de `size-touch-min` (44 × 44) y `MaterialTapTargetSize.padded`.
- **Pendientes ([POR DEFINIR]):** el pulsado del secundario usa `color-primary-tint` y el de peligro mantiene `color-danger` (más el ripple), hasta que la identidad defina esos tonos.
- **Pruebas:** `movil/test/shared/ui/app_button_test.dart` (cada variante: 44 × 44, `androidTapTargetGuideline`, `iOSTapTargetGuideline`, `labeledTapTargetGuideline`, `textContrastGuideline`; deshabilitado; cargando; anillo de foco).
- **Catálogo:** `flutter run` en `movil/` y «Ver catálogo de diseño» en la pantalla de inicio, o `flutter run --route /design-catalog` (solo en depuración).

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al pulsar, en Android y en iOS (no se usa el resaltado de Cupertino). Sin hover: el estado hover solo aplica con puntero |
