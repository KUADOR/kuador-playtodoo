# Cambios de despliegue · PLATODO-54

Baja definitiva de complejos (épica PLATODO-26, ADR-013).

## Base de datos

- Migración global nueva `V5__create_notification_outbox.sql`: crea `global.notification_outbox`. Se aplica sola al arrancar (Flyway `db/migration/global`). Es solo aditiva: no cambia tablas existentes.

## Configuración

| Propiedad | Valor por defecto | Qué hace |
|---|---|---|
| `tenancy.purge.enabled` (`TENANCY_PURGE_ENABLED`) | `true` | Activa el job diario de baja definitiva |
| `tenancy.purge.cron` (`TENANCY_PURGE_CRON`) | `0 0 3 * * *` | Hora del job, en UTC (cron de Spring: segundo, minuto, hora, día, mes, día de semana) |

- Sin secretos nuevos. Ningún entorno necesita cambiar los valores por defecto.
- El job es seguro con varias instancias de la api a la vez (`SELECT … FOR UPDATE SKIP LOCKED`): no hace falta elegir una instancia ni agregar ShedLock.
- **Irreversible:** el job elimina el esquema `t_<uuid>` de cada complejo `ARCHIVED` cuyo `purge_due_at` ya pasó. Antes del primer despliegue en producción, revisar que ningún complejo archivado tenga un plazo vencido por error: `SELECT id, display_name, purge_due_at FROM global.tenant WHERE status = 'ARCHIVED' AND purge_due_at <= now();`. Si hiciera falta frenar el job, desplegar con `TENANCY_PURGE_ENABLED=false`.
- Los respaldos de la base siguen teniendo los datos del complejo hasta que venza su retención: [POR DEFINIR: política de retención de respaldos frente a la baja definitiva].
- El correo al propietario queda en `global.notification_outbox` (tipo `TENANT_PERMANENTLY_DELETED`); todavía no se envía: llega con la épica de notificaciones (proveedor [POR DEFINIR]).

## Vuelta atrás

Revertir el PR desactiva el job. La tabla `notification_outbox` puede quedar (Flyway no baja versiones); las bajas definitivas ya hechas no se pueden deshacer.
