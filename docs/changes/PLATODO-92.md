# PLATODO-92 · Cambios para los documentos compartidos

Backoffice: auditoría de accesos (P7), acceder con motivo (P8) y vista de soporte en el host del complejo (P9). Al preparar el release, pasar estas notas a sus documentos y borrar este archivo.

## docs/architecture.md

En la sección de la pieza `backoffice`, agregar:

- `src/app/features/support`: P8 y P9 (ADR-020). `SupportAccessApi` (abrir y cerrar sesión, auditoría) en el host global y `SupportBookingsApi` (operaciones de personal de `bookings.openapi.yaml`) en el host del complejo, con el token de plataforma y la cabecera `X-Support-Session`. La URL del host la arma `TENANT_API_BASE_URL` a partir del `tenantHost` que devuelve la `api`.
- `SupportAccess` guarda en memoria, nunca en almacenamiento, la única sesión de soporte abierta (motivo, complejo y sesión; ningún dato operativo). Abrir otra cierra la anterior.
- P9 (`/complexes/{id}/support`) solo abre con una sesión de ese complejo (`supportAccessGuard`); si no, vuelve a P4. Sus datos viven solo en la página: se descartan al cerrar el acceso, al vencer la sesión, si la `api` la invalida (`SUPPORT_SESSION_INVALID`, `TENANT_UNAVAILABLE`), al salir de la vista o al abrir otro complejo. Nada se guarda en caché.
- El token de plataforma solo viaja a la `api` global y, con `X-Support-Session`, al host del complejo (`accessTokenInterceptor`).
- La pestaña «Auditoría» (`/complexes/{id}/audit`, solo SUPER_ADMIN) completa las pestañas del detalle.
- Puerto `SUPPORT_COURTS` para las canchas de un complejo: hoy no hay contrato (ADR-020, punto 6), así que en producción no hay canchas y «Crear reserva» y «Bloquear horario» quedan deshabilitados con su motivo. El mock de desarrollo trae canchas de ejemplo. [POR DEFINIR: endpoint de canchas, duración de franja y horario.]

## design-system/docs/components/calendar.md

En «Angular (`backoffice`)», reemplazar la «Diferencia aceptada» (siempre solo lectura, D6) por:

- `mode` (`readonly` por defecto, `editable`), `selected` y `(selectionChange)` existen como en el contrato. El modo editable lo usa la vista de soporte (ADR-020) para elegir la franja de «Crear reserva» y «Bloquear horario».
- En modo editable cada franja es un botón con `aria-pressed` (la seleccionada) y `aria-disabled` (las ocupadas); una sola franja en el orden de tabulación (la movida con el teclado, si no la seleccionada, si no la primera libre); flechas, Inicio y Fin para moverse; Enter o Espacio seleccionan y activar la seleccionada la quita. No muestra el aviso «Solo lectura».
- La franja seleccionada usa `color-primary` con check y texto `color-on-primary`; el foco, anillo `color-focus` con halo `color-focus-halo`.
- Las pruebas comprueban los fixtures en modo editable (selección efectiva, selección ignorada en ocupadas) además del modo de solo lectura.

## design-system/docs/components/field.md

Agregar la variante «Área de texto» (`kind="textarea"`, con `rows`): mismas reglas de etiqueta, ayuda, error, `maxlength` y `aria-describedby` que el campo de texto. La usa «Motivo del acceso» (P8). El campo de texto admite además `type="time"`.

## DESIGN.md

El botón admite un nombre accesible completo (`accessibleLabel`) cuando el texto visible se repite en la pantalla («Cancelar reserva de las 19:00, Cancha 1»). La barra de acceso de soporte usa `color-brand-navy` con texto y bordes `color-on-primary`.

## docs/screens/platodo-27-suscripciones-y-backoffice-de-kuador.md («Pantallas» P7 a P9)

- P9 en suspendidos y cancelados: aplicar los textos de «Diseño de la solución» v2 (acciones habilitadas para el SUPER_ADMIN, aviso fijo y etiqueta «Inactiva»), ya implementados.
- P9 «Crear reserva» y «Bloquear horario»: la cancha se elige en un selector y el horario en el calendario compartido en modo editable (vista diaria, navegable por día, semana y mes). Sin lista de canchas de la `api`, se deshabilitan con: «Todavía no podemos mostrar las canchas de este complejo, así que desde aquí no se pueden crear reservas ni bloquear horarios.».
- P9: la fila ya muestra todos los datos de la entrada; no hay un diálogo aparte de «Ver detalle».
- P7: «Acción» traduce el `operationId`; uno desconocido se muestra como «Otra operación» con método y plantilla de ruta.

## contracts (para Arquitectura; no se cambió)

1. No hay endpoint de canchas ni de horarios válidos (ya anotado en ADR-020): sin él, el soporte solo consulta y cancela.
2. El ejemplo de `listComplexAuditEntries` usa `operationId: cancelStaffBooking`, pero la operación de `bookings.openapi.yaml` se llama `cancelBookingByStaff`. El backoffice reconoce las dos; conviene unificar el ejemplo.
3. Para que el navegador llame al host del complejo desde `admin.{raíz}`, el CORS de la `api` debe permitir la cabecera `X-Support-Session` (hoy permite `X-Platform-Access-Session`). Lo cubre PLATODO-88 al reemplazar la cabecera.
