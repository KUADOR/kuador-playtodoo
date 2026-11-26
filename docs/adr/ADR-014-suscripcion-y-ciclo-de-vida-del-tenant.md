# ADR-014 · Estado de la suscripción y ciclo de vida del tenant separados

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador («Diseño de la solución» v2).
- **Ajusta:** ADR-013 (estados del tenant y comienzo del plazo de conservación).
- **Contrato:** `contracts/platform-backoffice.openapi.yaml` (etiquetas «Complexes» y «Subscriptions»).

## Contexto

Kuador lleva a mano el estado comercial de cada complejo: activa al suscribirse y el resto de los cambios manuales, porque la forma de pago aún no está definida. El Equipo Kuador decidió que una suscripción suspendida bloquea a todos salvo a Kuador, que «Cancelada» bloquea igual que una suspensión y corre el plazo de conservación sin poder reactivar (respuesta 1, opción A) y que al vencer el plazo se borra todo.

Hoy el ciclo de vida del tenant (ADR-013) lo cambia Kuador directamente (`suspend`, `archive`, `reactivate`) y solo `ARCHIVED` inicia el plazo.

## Decisión

1. **Dos modelos.** La suscripción (módulo Planes y suscripciones) tiene un estado comercial: `ACTIVE`, `EXPIRING`, `SUSPENDED`, `CANCELLED`. El tenant (Tenancy) tiene el ciclo de vida que decide acceso y datos: `PROVISIONING`, `PROVISIONING_FAILED`, `ACTIVE`, `SUSPENDED`, `ARCHIVED`, `PURGING`, `PURGED`.
2. **Un solo punto de entrada.** El backoffice solo cambia la suscripción (`POST /complexes/{complexId}/status-changes`). El módulo publica `SubscriptionStatusChanged` y Tenancy lo escucha **de forma síncrona, en la misma transacción**, y aplica el efecto. Si Tenancy falla, se revierte también el cambio comercial.
3. **Transiciones (siempre manuales, ningún proceso cambia la suscripción):**

   | Desde | Hacia | Efecto en el tenant |
   |---|---|---|
   | (alta) | `ACTIVE` | `ACTIVE` al terminar de crear y migrar el esquema |
   | `ACTIVE` ⇄ `EXPIRING` | | Ninguno |
   | `ACTIVE`, `EXPIRING` | `SUSPENDED` | `SUSPENDED`, `purge_due_at = suspended_at + retention_days` |
   | `ACTIVE`, `EXPIRING` | `CANCELLED` | `ARCHIVED`, `purge_due_at = archived_at + retention_days` |
   | `SUSPENDED` | `ACTIVE` | Solo si `now < purge_due_at`; vuelve a `ACTIVE` y borra el plazo |
   | `SUSPENDED` | `CANCELLED` | `ARCHIVED` conservando el `purge_due_at` que ya corría |
   | `CANCELLED` | — | Terminal: no se reactiva |

4. **Bloqueo igual en los dos.** `SUSPENDED` y `ARCHIVED` responden «complejo no disponible» (`403 TENANT_UNAVAILABLE`) a personal y jugadores y el complejo sale del catálogo (`TenantStatusChanged` síncrono hacia Complejos y canchas). Solo pasa una sesión de soporte de un `SUPER_ADMIN` (ADR-020).
5. **Alta incompleta.** Si no se puede crear el esquema, el tenant queda `PROVISIONING_FAILED` y se reintenta con `POST /complexes/{complexId}/provisioning-retries` (idempotente). La suscripción se crea `ACTIVE` junto al registro del tenant, sin transiciones permitidas hasta que el tenant esté `ACTIVE`.
6. **Concurrencia.** Toda mutación sobre un complejo lleva `version`; si no coincide, `409 STALE_STATE`.
7. **Operaciones reemplazadas.** `suspend`, `activate`, `archive`, `reactivate` y `retention` de `tenancy.openapi.yaml` quedan `deprecated` y se quitan al implementar PLATODO-84 a PLATODO-86.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Un solo estado compartido por suscripción y tenant | Mezcla reglas comerciales con aislamiento y obliga a Reservas y Tenancy a conocer conceptos de facturación |
| Evento asíncrono entre suscripción y tenant | Podría quedar una suscripción suspendida con el complejo todavía abierto |
| Mantener los cambios directos del tenant además de la suscripción | Dos caminos para el mismo efecto terminan desalineados |
| Cancelada sin efecto en el acceso (solo comercial) | Descartada por el Equipo Kuador (respuesta 1) |

## Consecuencias

- El tenant ya no se reactiva desde `ARCHIVED`: cambia ADR-013.
- La purga (ADR-015) cambia solo el estado del tenant, nunca el de la suscripción.
- Prueba de rollback: si Tenancy falla al aplicar el efecto, la suscripción no cambia.
