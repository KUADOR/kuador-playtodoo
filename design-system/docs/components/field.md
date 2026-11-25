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
