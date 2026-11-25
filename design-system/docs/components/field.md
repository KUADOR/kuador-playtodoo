# Field · campo de formulario

> Esqueleto del contrato. Las tareas de cada pieza (PLATODO-18, PLATODO-20 y PLATODO-22) completan ejemplos y capturas; PLATODO-24 cierra la documentación final.

Contrato común de `Field` (`app`, `backoffice`) y `AppField` (`movil`). Solo usa tokens de `design-system/tokens`.

## Uso

Captura de un dato. La etiqueta siempre está visible sobre el campo; la ayuda es opcional y va debajo.

## Propiedades

| Propiedad | Tipo | Regla |
|---|---|---|
| `kind` | `text` · `select` | Tipo de campo |
| `label` | texto | Siempre visible; es el nombre accesible |
| `help` | texto opcional | Debajo del campo |
| `error` | texto opcional | Dice qué pasó y qué hacer, sin culpar. Ej.: «Ese horario ya fue reservado. Elige otro disponible.» |
| `disabled` | booleano | No editable |
| `options` | `{ value, label }[]` | Solo `select` |

| Evento | Cuándo |
|---|---|
| `change` / `onChanged` (según la plataforma) | Al cambiar el valor |

## Estados

| Estado | Aspecto |
|---|---|
| Vacío | Borde `color-fg-muted` de `border-width-sm` |
| Con valor | Igual que vacío, con el valor en `color-fg` |
| Foco | Borde `color-primary` de `border-width-lg` + anillo `color-focus` |
| Error | Borde `color-danger` de `border-width-lg` + icono de alerta + mensaje en `color-danger` debajo |
| Deshabilitado | Fondo `color-surface-alt` |

Medidas: alto mínimo `size-touch-min`; radio `radius-md`; texto mínimo `font-size-sm` en web y `font-size-md` en móvil.

## Accesibilidad

El mensaje de error y la ayuda se asocian al campo (`aria-describedby` en web, `Semantics` en Flutter); el error también se marca con `aria-invalid`.

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Teclado en pantalla y selectores nativos |
