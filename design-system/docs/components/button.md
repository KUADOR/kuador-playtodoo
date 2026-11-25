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

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Ripple de Material al pulsar |

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
