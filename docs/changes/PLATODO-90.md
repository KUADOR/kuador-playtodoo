# PLATODO-90 · Cambios para los documentos compartidos

Backoffice: complejos (P2), alta (P3), detalle con Resumen y Pagos (P4), diálogos de plan, estado, pago y plazo (P5) e historial (P6). Al preparar el release, pasar estas notas a sus documentos y borrar este archivo.

## docs/architecture.md

En la sección de la pieza `backoffice`, agregar:

- `src/app/features/complexes`: P2 a P6. Un puerto por grupo de operaciones del contrato (`ComplexesApi`, `SubdomainsApi`, `PublishedPlansApi`). El detalle (P4) comparte el complejo abierto con sus pestañas y diálogos mediante `ComplexDetailStore`, provisto por la página: vive mientras el complejo está abierto y las pestañas se recargan por contadores de revisión (sin estado global).
- Cada pestaña del detalle tiene dirección propia: `/complexes/{id}`, `/complexes/{id}/payments` y `/complexes/{id}/history` (la pestaña «Auditoría» la agrega PLATODO-92).
- El dominio raíz de los subdominios sale del host del backoffice (`admin.{raíz}` → `{raíz}`); en otro host, el propio host (p. ej. `{subdominio}.localhost`).
- Estados comunes reutilizables: `PagedList` (listas paginadas) y `RemoteValue` (un valor leído) en `src/app/core/api`, con carga, error común con «Reintentar» y datos.
- El aviso de resultado tras navegar («Dimos de alta…») viaja en el estado de la navegación (`withResultNotice`): se muestra una vez y no reaparece al recargar.

## DESIGN.md

Componentes de la v3 implementados en `backoffice/src/app/shared/ui` solo con tokens existentes: «Etiqueta de estado» (`status-tag`) y «Pestañas» con dirección propia (`tab-nav`). El campo admite sufijo fijo (p. ej. `.playtodoo.com`), fecha nativa y `inputmode`. Se agregó `.link-button` (enlace con aspecto de botón primario) y `.text-link` en `src/styles.css`.

## contracts/platform-backoffice.openapi.yaml (para Arquitectura; no se cambió)

Huecos encontrados al construir las pantallas. No bloquean; la pantalla hace lo indicado mientras tanto:

1. `PlanSnapshot.customFields` no trae la etiqueta de cada campo y `GET /plan-fields` es solo del SUPER_ADMIN: el Resumen y la comparación de planes muestran la clave (`support-hours`).
2. Antes de suspender no se conoce el plazo de conservación por defecto (`retentionDays`): el diálogo «Suspender complejo» avisa que empieza el plazo, sin días ni fecha; la fecha exacta aparece en el detalle al confirmar. «Pantallas» pide mostrarlos antes.
3. `ComplexDetail` no trae el último pago: el Resumen lo pide con `GET /payments?size=1`.
