# ADR-015 · Purga automática al vencer el plazo de conservación

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero; puede revertirse sin rehacer el resto).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Ajusta:** ADR-013 (punto 4, purga).

## Contexto

El Equipo Kuador pidió que al vencer el plazo de conservación (45 días por defecto, configurable) de un complejo suspendido o cancelado «se debe borrar todo». Quedó sin respuesta, tras preguntarse dos veces, si el borrado es automático o espera la confirmación de un `SUPER_ADMIN`.

## Decisión

1. **Plazo desde la suspensión o la cancelación.** `retention_days` toma el parámetro `retention.default-days` (45) al entrar en `SUSPENDED` o `ARCHIVED`. El `SUPER_ADMIN` lo puede cambiar por complejo mientras esté en uno de esos dos estados (`PUT /complexes/{complexId}/retention`); el nuevo `purge_due_at` debe quedar en el futuro. Si un suspendido se cancela, conserva su fecha.
2. **Borrado automático.** La tarea programada diaria (UTC) de Tenancy purga los tenants `SUSPENDED` o `ARCHIVED` con `purge_due_at <= now()`, sin confirmación:
   1. Los toma con `FOR UPDATE SKIP LOCKED`, los pasa a `PURGING` e invalida sus sesiones de soporte en la misma transacción. Desde ese commit el filtro rechaza toda solicitud y ya no se puede reactivar (`422 RETENTION_EXPIRED`).
   2. `DROP SCHEMA IF EXISTS <nombre> CASCADE` con el nombre tomado del registro global y validado por patrón, nunca de una entrada.
   3. Completa la baja según ADR-016 y pasa a `PURGED`.
   4. Es idempotente: un tenant que quedó en `PURGING` se retoma en la siguiente ejecución. Se puede desactivar por configuración.
3. **Aviso previo.** El backoffice muestra la fecha de borrado y destaca los complejos a 7 días o menos; el `SUPER_ADMIN` puede ampliar el plazo.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Confirmación manual del `SUPER_ADMIN` antes de borrar | Deja datos indefinidamente si nadie confirma, contra «se debe borrar todo». Si la puerta la prefiere, solo cambian PLATODO-87 (estado intermedio a la espera de confirmación) y la pantalla P4 |
| Borrar al instante al cancelar | El equipo eligió conservar durante el plazo (respuesta 1, opción A) |
| ShedLock para la tarea | `SKIP LOCKED` sobre la propia tabla basta (ADR-013) |

## Consecuencias

- Es irreversible: pruebas de que no se purga antes de `purge_due_at`, fuera de `SUSPENDED` o `ARCHIVED`, ni otro esquema que el objetivo; prueba concurrente de reactivar contra purgar.
- El correo al propietario de ADR-013 sigue encolado en `notification_outbox`; su envío llega con la épica de notificaciones.
