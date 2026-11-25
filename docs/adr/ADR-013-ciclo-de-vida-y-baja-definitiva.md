# ADR-013 · Ciclo de vida del complejo y baja definitiva con conservación anonimizada

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Épica:** PLATODO-26 · Aislamiento de datos por complejo (decisiones D8, D9, D10 y D11 del diseño de la solución).
- **Contrato:** `contracts/tenancy.openapi.yaml` (etiqueta «Platform tenants»).

## Contexto

Decisiones del Equipo Kuador:

- Un complejo suspendido muestra «complejo no disponible» a todas las personas.
- Al darse de baja, el complejo queda **archivado 45 días** (plazo modificable por Kuador). En ese plazo Kuador puede reactivarlo y recupera sus datos.
- Al vencer el plazo, se envía un correo al **propietario** y se **eliminan completamente** los datos del complejo.
- Tras la baja definitiva se conservan, **sin datos personales**, la suscripción y sus pagos (son parte de las métricas del sistema) y la auditoría (opción B).

## Decisión

1. **Estados:** `PROVISIONING → ACTIVE ⇄ SUSPENDED`, `ACTIVE | SUSPENDED → ARCHIVED`, `ARCHIVED → ACTIVE` (reactivar antes de `purge_due_at`), `ARCHIVED → PURGING → PURGED`. Cada transición publica su evento `Tenant*` (ver «Catálogo de eventos» en Arquitectura).
2. **Respuestas del host del complejo (D9):** `SUSPENDED` y `ARCHIVED` responden `403 TENANT_UNAVAILABLE`, sin decir cuál de los dos ni el plazo; inexistente, retirado, `PURGING` o `PURGED` responden `404 TENANT_NOT_FOUND`.
3. **Plazo de archivado.** Al archivar, `retention_days` toma `tenancy.archive.default-retention-days` (45) y `purge_due_at = archived_at + retention_days`. Kuador lo puede cambiar mientras esté `ARCHIVED` (mínimo 1 día; el nuevo `purge_due_at` debe quedar en el futuro). Máximo: [POR DEFINIR].
4. **Purga (D8).** Job diario (UTC), seguro con varias instancias:
   1. Toma los `ARCHIVED` con `purge_due_at <= now` con `SELECT … FOR UPDATE SKIP LOCKED` y los pasa a `PURGING`.
   2. Dentro del ámbito del tenant (ADR-010) lee el correo del propietario y lo encola en `notification_outbox` mediante el puerto `TenantOwnerNotifier`.
   3. `DROP SCHEMA IF EXISTS <nombre validado> CASCADE`.
   4. En una transacción: `PURGED` con `purged_at`, retira sus subdominios (ADR-012) y publica `TenantPurged`.
   5. Si algo falla, queda en `PURGING` y el job lo reintenta. Cada paso es idempotente.
5. **Qué se conserva (D10, opción B):**

   | Dato | Resultado |
   |---|---|
   | Esquema `t_<uuid>` (personal, canchas, reservas, novedades, torneos) | Eliminado |
   | `tenant` | Lápida: id, nombre comercial, estado `PURGED` y fechas |
   | `tenant_domain` | Retirado, no reutilizable |
   | Auditoría de accesos | Se conserva; el motivo se reemplaza por «[anonimizado por baja definitiva]» y se marca `reason_redacted_at` |
   | Suscripción y pagos | Se conservan para métricas, sin datos personales (lo hace su módulo) |
   | `player_account` | Sin cambios (es global) |

6. **Cada módulo anonimiza lo suyo (D11).** Los listeners de `TenantPurged` de Auditoría y de Planes y suscripciones son **síncronos y corren dentro de la transacción de purga**: si fallan, el tenant no queda `PURGED`. Es la excepción a la regla general de escuchar eventos `AFTER_COMMIT`.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Eliminar todo, incluidos suscripción, pagos y auditoría (opción A) | Rompe las métricas de pagos y la trazabilidad de los accesos de Kuador |
| Que Tenancy anonimice tablas de otros módulos | Rompe las fronteras del monolito modular (ADR-007) |
| Listener asíncrono de `TenantPurged` | Podría quedar `PURGED` con datos personales sin anonimizar |
| ShedLock para el job | Dependencia extra; `SKIP LOCKED` sobre la propia tabla basta |
| Enviar el correo directamente | El proveedor de correo sigue [POR DEFINIR]; el outbox lo desacopla |
| `503` para complejo no disponible | Invita a reintentar y los proxies lo pueden cachear |

## Consecuencias

- La baja definitiva es irreversible: se prueba que no se purga antes de `purge_due_at` ni fuera de `ARCHIVED`, y el nombre del esquema se valida antes del `DROP`.
- **Condición para la épica de Planes y suscripciones:** escuchar `TenantPurged` de forma síncrona y quitar los datos personales de contacto o facturación, con su propia prueba.
- El correo se envía cuando llegue la épica de notificaciones; hasta entonces queda en el outbox.
