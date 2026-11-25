# Cambios a documentos compartidos · PLATODO-22

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## docs/architecture.md

- En la tabla de piezas, fila `movil`: compilar `flutter analyze`, probar `flutter test`, ejecutar `flutter run`. Son los mismos comandos que propone PLATODO-17.
- En «Piezas y quién las usa», fila `movil`: el proyecto Flutter existe desde PLATODO-22, con el identificador **com.kuador.playtodoo** en Android e iOS, el paquete Dart `playtodoo`, Flutter 3.50.1 y `flutter_lints`. Siguen [POR DEFINIR] la organización de capas y la gestión de estado. Estructura inicial:
  - `lib/core/design/`: tokens generados, `AppTheme`, `AppColors` e `AppIcon`;
  - `lib/shared/ui/`: componentes base;
  - `lib/features/<feature>/`: pantallas;
  - `lib/app/`: rutas.
- Desde Flutter 3.47 los widgets de Material se importan desde el paquete `material_ui` (`package:material_ui/material_ui.dart`). `package:flutter/material.dart` está deprecado.

## AGENTS.md

En «Comandos del proyecto», además de los comandos de PLATODO-17 para `movil`:

```bash
flutter run                           # levantar en un emulador o simulador
flutter run --route /design-catalog   # abrir el catálogo de diseño (solo en depuración)
```

## README.md

En la pieza `movil`, indicar que requiere Flutter 3.50.1 estable. Para compilar Android hace falta JDK 17 o posterior, y para iOS, Xcode en macOS.
