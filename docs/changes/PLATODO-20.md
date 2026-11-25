# Cambios a documentos compartidos · PLATODO-20

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## docs/architecture.md

En la fila de `backoffice` de «Arquitectura del frontend», reemplazar parte del [POR DEFINIR] de organización:

- Proyecto Angular 22 creado con Angular CLI (componentes standalone, sin SSR, pruebas con Vitest mediante `ng test`). Antes de esta tarjeta la carpeta `backoffice/` no tenía proyecto.
- Componentes base compartidos en `src/app/shared/ui/<componente>/` (`Button`, `Field`, `Card`); tokens y fuentes generados por `design-system/` en `src/styles/` y cargados desde `styles` de `angular.json`.
- Páginas solo de desarrollo en `src/app/dev/`, registradas en `src/app/dev-routes.development.ts`. `angular.json` reemplaza `dev-routes.ts` (vacío) por ese archivo solo en la configuración `development`, así no llegan al bundle de producción. Hoy: catálogo `/design`.
- Iconos: `@lucide/angular` (paquete oficial actual de Lucide para Angular). El plan nombraba `lucide-angular`, pero su última versión (1.0.0) solo admite Angular hasta 21.
- Pruebas de accesibilidad con `axe-core` (helper `src/testing/axe.ts`).
- Gestión de estado y acceso a datos siguen [POR DEFINIR].

## AGENTS.md

Sin cambios en los comandos: `npm run build`, `npm test -- --watch=false` y `npm start` funcionan tal cual. Opcional en «Comandos del proyecto»: `npx prettier --check src` (formato, lo corre el CI de la pieza).

## README.md

Sin cambios.
