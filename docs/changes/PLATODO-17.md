# Cambios a documentos compartidos · PLATODO-17

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## AGENTS.md

En «Comandos del proyecto», reemplazar el [POR DEFINIR] de `movil` por los comandos propuestos (los mismos que corre `.github/workflows/movil.yml` con Flutter 3.50.1 estable):

```bash
# movil (movil/) · jugadores
flutter pub get        # dependencias
flutter analyze        # compilar (análisis estático y tipos)
flutter test           # probar
```

## Piezas del tablero (las registra una persona)

En la pieza `movil`: compilar `flutter analyze`, probar `flutter test`, ejecutar `flutter run`. Versión de Flutter fijada: 3.50.1 (canal stable), definida en `FLUTTER_VERSION` de `.github/workflows/movil.yml`.

## README.md

En la sección de CI, mencionar los workflows por carpeta: `design-system.yml` (tokens:check, pruebas y contraste) y `movil.yml` (flutter analyze y flutter test).
