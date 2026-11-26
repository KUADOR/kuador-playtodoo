# PLATODO-91 · Cambios para los documentos compartidos

Backoffice: planes (P10), crear o editar plan (P11) y campos de plan (P12). Al preparar el release, pasar estas notas a sus documentos y borrar este archivo.

## docs/architecture.md

En la sección de la pieza `backoffice`, agregar:

- Rutas de planes (solo `SUPER_ADMIN`): `/plans` (P10), `/plans/new` y `/plans/{planId}` (P11) y `/plans/fields` (P12). P11 usa la guarda `unsavedChangesGuard` (`core/navigation`): salir con cambios sin guardar pide confirmación.
- `src/app/features/plans` agrupa los puertos `PlansApi` (etiqueta «Plans») y `PlanFieldsApi` («Plan fields») y el modelo del formulario `plan-form.ts`, que genera los controles de «Otros campos» según el tipo del campo (Texto, Número, Sí/No). Otras pantallas que muestren un plan (P3, P5) pueden reutilizar `plan-texts.ts` (precio con moneda, periodicidad y límites).
- Componentes nuevos de la Identidad visual v3 en `shared/ui`: `status-badge` («Etiqueta de estado») y `link-button` (enlace con aspecto de botón). `button` acepta `accessibleLabel` y `field` acepta `inputMode`.
- El mock en memoria del contrato se divide por grupo de operaciones: `dev/mock-backend/mock-plans-backend.ts` (planes y campos) con ayudas comunes en `mock-responses.ts`.

## docs/glossary.md

- **Campo básico**: campo de plan sembrado por la api (nombre, precio, moneda, periodicidad, límites); no se edita ni se retira.
- **Campo adicional**: campo de plan creado por Kuador en «Campos de plan»; solo describe el plan, no activa funciones ni límites por sí solo.
- **Campo retirado**: campo adicional que deja de pedirse en los planes; sus valores guardados no se borran.
