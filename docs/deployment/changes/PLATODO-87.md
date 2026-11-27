# Despliegue · PLATODO-87

Sin servicios, secretos, puertos ni migraciones nuevas. Cambia el alcance de una tarea programada y se agregan propiedades.

## Qué cambia

- **Purga (`tenancy.purge.*`, ya existía):** ahora también borra los complejos **suspendidos** cuyo plazo de conservación venció, además de los cancelados (ADR-015). Al tomarlos (paso a `PURGING`) cierra sus sesiones de soporte abiertas y, al terminar, borra su fila de `complex_catalog`. Sigue diaria a las 03:00 UTC y se desactiva con `tenancy.purge.enabled=false`.
- **Retención de 1 año (nuevo, ADR-016):** una tarea diaria por módulo borra lo que supera el plazo:

  | Propiedad | Valor por defecto | Qué borra |
  |---|---|---|
  | `playtodoo.audit.retention-days` | `365` | `access_audit_event` y las `access_session` vencidas sin eventos |
  | `playtodoo.history.retention-days` | `365` | `subscription_history` y `capability_change` (también acota sus listados) |
  | `playtodoo.retention.enabled` | `true` | `false` apaga las tres tareas |
  | `playtodoo.retention.cron` | `0 30 3 * * *` (UTC) | Horario de las tres tareas |

  Variables de entorno equivalentes, si se quieren cambiar: `PLAYTODOO_AUDIT_RETENTIONDAYS`, `PLAYTODOO_HISTORY_RETENTIONDAYS`, `PLAYTODOO_RETENTION_ENABLED`, `PLAYTODOO_RETENTION_CRON`.

## Qué hacer en cada entorno

- **Local, staging y producción:** nada obligatorio; los valores por defecto son los decididos por el equipo Kuador (1 año).
- **Antes del primer despliegue en un entorno con datos:** revisar si hay complejos `SUSPENDED` con `purge_due_at` vencido; la primera ejecución los borrará de forma definitiva.

  ```sql
  SELECT id, display_name, purge_due_at FROM global.tenant
  WHERE status = 'SUSPENDED' AND purge_due_at <= now();
  ```

  Si alguno no debe borrarse, ampliar su plazo desde el backoffice antes de las 03:00 UTC, o desplegar con `TENANCY_PURGE_ENABLED=false` hasta decidir.
- Las tareas son seguras con varias instancias de la api a la vez.
