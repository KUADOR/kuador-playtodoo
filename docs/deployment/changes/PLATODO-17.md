# Cambios de despliegue · PLATODO-17

## CI/CD

Dos workflows nuevos, solo de verificación (no publican ni despliegan nada):

| Workflow | Se activa con | Qué hace |
|---|---|---|
| `.github/workflows/design-system.yml` | PR o push a develop/main que toque `design-system/` o los archivos generados en `app`, `backoffice` y `movil` (lista espejo de `design-system/build/targets.js`) | `npm ci`, `npm run tokens:check` (falla si un archivo generado difiere de los tokens), `npm test` (pruebas y contraste WCAG) y `npm run build` sin cambios en el repo. Node 22 |
| `.github/workflows/movil.yml` | PR o push a develop/main que toque `movil/` | Con Flutter 3.50.1 estable: `flutter pub get`, `flutter analyze`, `flutter test`. Mientras no exista `movil/pubspec.yaml` deja un aviso y no corre los pasos de Flutter |

- Sin variables ni secretos nuevos. Sin cambios en pruebas ni producción.
- Ramas protegidas: no agregar estos jobs como checks obligatorios, porque al estar filtrados por ruta no corren en PR que no tocan sus carpetas y el PR quedaría bloqueado. El check obligatorio sigue siendo `ci`.
- Subir la versión de Flutter: cambiar `FLUTTER_VERSION` en `movil.yml` en un PR propio.

## Vuelta atrás

Borrar el workflow afectado en un PR a develop. No hay estado que restaurar.
