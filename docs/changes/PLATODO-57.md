# Cambios a documentos compartidos · PLATODO-57

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## AGENTS.md

En «Comandos del proyecto», reemplazar el `[POR DEFINIR]` de `movil` por estos comandos (los mismos que corre `.github/workflows/movil.yml`, con Flutter 3.50.1 estable; coinciden con PLATODO-17, PLATODO-22 y PLATODO-34). Verificados en verde en esta tarea: `flutter analyze` sin problemas y `flutter test` con 151 pruebas.

```bash
# movil (movil/) · jugadores · Flutter 3.50.1 estable
flutter pub get          # dependencias
flutter analyze          # compilar (análisis estático y tipos)
flutter test             # probar
flutter run              # levantar en un emulador o simulador
```

Con una versión anterior de Flutter, `flutter pub get` falla: `pubspec.yaml` exige `flutter >=3.50.1`.

## Piezas del tablero (las registra una persona)

En la pieza `movil`: compilar `flutter analyze`, probar `flutter test`, ejecutar `flutter run`.

## docs/architecture.md

En «Clientes», fila `movil`: la entrada a un complejo pasa por `TenantGate` (`lib/features/tenant/`), que consulta `GET /api/v1/tenant` y muestra las pantallas del complejo solo si está activo:

- `403 TENANT_UNAVAILABLE` → «Complejo no disponible».
- `404 TENANT_NOT_FOUND` → «Complejo no encontrado».
- Sin conexión o una respuesta fuera del contrato → «No pudimos comprobar el complejo», con Reintentar.

Las pantallas del complejo viven en un `Navigator` propio de la puerta. Si el complejo deja de estar activo, la puerta reemplaza esa navegación completa por la pantalla de estado. La puerta vuelve a comprobar el complejo al volver la app a primer plano y cuando una pantalla del complejo llama a `TenantGate.recheck`. Una comprobación fallida con el jugador adentro no lo saca: solo lo saca una respuesta sobre el complejo.

Hoy funciona contra el mock del contrato (`InMemoryTenantApi`). El adaptador HTTP llega con la tarea de integración.

## DESIGN.md

- Igual que en PLATODO-56: [POR DEFINIR: token de ancho de las tarjetas de estado], hoy 480 px según Pantallas PLATODO-26.
- Igual que en PLATODO-56: [POR DEFINIR: logo]. Mientras no haya logo, las pantallas de estado muestran el nombre «Playtodoo» en la tipografía de títulos.
