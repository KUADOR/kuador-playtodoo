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

## React (`app`)

Ubicación: `app/src/shared/ui/field/Field.tsx` (exportado desde `app/src/shared/ui`). Estilos en `Field.module.css`, solo con `var(--…)`.

```tsx
import { Field } from '../shared/ui'

<Field kind="text" label="Nombre del complejo" help="Así lo verán los jugadores." value={name} onChange={(e) => setName(e.target.value)} />
<Field kind="text" label="Horario" value={time} error="Ese horario ya fue reservado. Elige otro disponible." />
<Field kind="select" label="Cancha" placeholder="Elige una cancha" options={[{ value: 'court-1', label: 'Cancha 1' }]} />
<Field kind="select" label="Cancha" options={courts} disabled />
```

| Contrato | React |
|---|---|
| `kind` | `kind` (`text` → `<input>`, `select` → `<select>`) |
| `label`, `help`, `error`, `disabled` | Mismos nombres |
| `options` | `options: { value, label }[]`; `placeholder` opcional agrega una primera opción vacía |
| `change` | `onChange` nativo del control |

Además acepta los atributos del `<input>` o del `<select>` (`value`, `defaultValue`, `name`, `placeholder`, `required`…), salvo `id`, `aria-describedby`, `aria-invalid` y `disabled`, que maneja el componente.

- **Asociación:** `<label for>` con un `id` de `useId`; ayuda y error se enlazan con `aria-describedby` (primero la ayuda, después el error) y el error marca `aria-invalid="true"`.
- **Error:** borde `color-danger` de `border-width-lg`, icono Lucide `CircleAlert` (decorativo, `aria-hidden`) y mensaje en `color-danger`.
- **Foco:** borde `color-primary` de `border-width-lg` + anillo `color-focus` con halo.
- **Texto:** `font-size-md` en el control (también evita el zoom del navegador móvil); etiqueta, ayuda y error en `font-size-sm`.
- **Pruebas:** `Field.test.tsx` (etiqueta, cambios, ayuda, error asociado, ids únicos, deshabilitado; axe en cada estado de texto y de selección).
- **Catálogo:** `/dev/design`, sección «Campo».

## Flutter (`movil`)

`movil/lib/shared/ui/app_field.dart`. `AppField.text` usa `TextField` y `AppField.select` usa `DropdownButtonFormField`, los dos con el `InputDecorationTheme` de `AppTheme.light()`.

```dart
AppField.text(label: 'Correo', keyboardType: TextInputType.emailAddress, onChanged: setEmail);
AppField.text(label: 'Correo', errorText: 'Ese correo no es válido. Revisa que tenga @.');
AppField<String>.select(
  label: 'Deporte',
  options: const [AppSelectOption(value: 'futbol', label: 'Fútbol')],
  value: sport,
  onChanged: setSport,
);
```

| Contrato | Flutter |
|---|---|
| `kind` | Constructor `AppField.text` o `AppField.select` |
| `label` | `label`: texto encima del campo (no etiqueta flotante) |
| `help` | `helperText` |
| `error` | `errorText`: borde `color-danger` de `border-width-lg`, icono de alerta y mensaje debajo |
| `disabled` | `enabled: false` (fondo `color-surface-alt`) |
| `options` | `options: List<AppSelectOption<T>>` |
| `change` | `onChanged` |

- **Semantics:** la etiqueta, el error y la ayuda forman un solo nodo (`MergeSemantics`): el lector dice «Correo, campo de texto, Ese correo no es válido…». Las copias visibles del error y la ayuda se excluyen para no leerlas dos veces.
- **Selección:** se usa `DropdownButtonFormField` en lugar de `DropdownMenu` porque acepta el mismo `InputDecoration` (con el error como widget, con su icono), y el campo de texto y el de selección quedan iguales.
- **Pruebas:** `movil/test/shared/ui/app_field_test.dart` (etiqueta encima; error con borde, icono, mensaje y Semantics; selección que emite el valor; deshabilitado; 44 de alto).

## Diferencias aceptadas por plataforma

| Plataforma | Diferencia |
|---|---|
| `movil` | Teclado en pantalla del sistema según `keyboardType` (correo, número, teléfono). La selección abre el menú de Material en las dos plataformas. Fecha y hora (fuera de esta tarea) usarán los selectores nativos de cada plataforma |

## Angular (`backoffice`)

Componente standalone `Field` en `backoffice/src/app/shared/ui/field/` (selector `app-field`).

| Contrato | Angular |
|---|---|
| `kind`, `label`, `help`, `error`, `disabled`, `options` | Entradas con el mismo nombre (`label` es obligatoria); `options` es `{ value, label }[]` |
| `change` | `value` es un `model()`: enlace doble `[(value)]` o salida `(valueChange)` al escribir o elegir |
| — | `placeholder`: texto de ejemplo en `text`; en `select` es una primera opción deshabilitada, elegida mientras no hay valor |

```html
<app-field label="Nombre del complejo" [(value)]="name" help="Como aparece para los jugadores." />
<app-field
  kind="select"
  label="Plan"
  placeholder="Elige un plan"
  [options]="plans"
  [(value)]="plan"
  [error]="planError()"
/>
```

- **Etiqueta:** `<label for>` visible, enlazada a un id único por campo.
- **Error:** borde `color-danger` de `border-width-lg`, icono `LucideCircleAlert` y mensaje en `color-danger`; el control lleva `aria-invalid="true"` y `aria-describedby` con los ids de la ayuda y del error.
- **Foco:** borde `color-primary` de `border-width-lg` + anillo `color-focus`. Con error, el borde sigue en `color-danger`.
- El grosor del borde de foco y error se compensa en el relleno para que el texto no se mueva.
- **Selección:** `<select>` nativo del navegador (flecha y lista del sistema).
- Validar es responsabilidad de la pantalla: el campo solo muestra el `error` que recibe.
- Catálogo vivo en `/design` (solo desarrollo). Capturas en `docs/evidence/platodo-20/`.
