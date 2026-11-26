# ADR-016 · Alcance de «borrar todo» en la baja definitiva

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Ajusta:** ADR-013 (puntos 5 y 6).

## Contexto

El equipo pidió borrar todo al vencer el plazo y, a la vez, conservar la auditoría y el historial de cambios de plan durante 1 año. ADR-013 ya conserva sin datos personales la suscripción, los pagos y la auditoría.

## Decisión

1. **Se borra todo lo del complejo:** su esquema completo (personal, canchas, reservas, novedades, torneos) y su fila de `complex_catalog`.
2. **Se redactan los datos personales del esquema global:** `tenant.owner_email` y los motivos de las sesiones de soporte (`reason_redacted`). El subdominio queda retirado, no reutilizable (ADR-012).
3. **Sobreviven solo los registros de Kuador:** la lápida del `tenant`, la suscripción con su instantánea de plan, los pagos, el historial y la auditoría. Historial, auditoría y cambios de capacidades se borran al cumplir 365 días (`history.retention-days`, `audit.retention-days`) por una tarea programada. Plazo legal de los pagos: [POR DEFINIR: obligaciones del país].
4. **Cada módulo redacta lo suyo.** Se mantiene ADR-013: los consumidores de `TenantPurged` (Auditoría y Planes y suscripciones) escuchan **de forma síncrona dentro de la transacción que pasa el tenant a `PURGED`**; si uno falla, el tenant queda en `PURGING` y la tarea reintenta. Esto precisa el «después del commit» del diseño v2: ya está implementado así y es más seguro.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Borrar también la suscripción, los pagos y la auditoría | Incumple la auditoría y el historial de 1 año pedidos por el equipo |
| Listener asíncrono de `TenantPurged` | Podría quedar `PURGED` con datos personales sin redactar |
| Que Tenancy borre tablas de otros módulos | Rompe las fronteras del monolito modular (ADR-007) |

## Consecuencias

- El listado y el detalle de un complejo purgado siguen disponibles con `subdomain` y `ownerEmail` nulos.
- Pruebas: tras la purga no queda el esquema ni la fila del catálogo, los datos personales del global están redactados y otro tenant queda intacto.
