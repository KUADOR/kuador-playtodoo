# Cambios a documentos compartidos · PLATODO-85

## docs/architecture.md

En la sección «Módulos», fila «Planes y suscripciones», agregar lo que suma PLATODO-85 (ADR-014, ADR-018):

```
com.kuador.platodo.api.subscriptions/
  domain/                        Subscription.changeStatus / changePlan, TransitionNotAllowedException,
                                 SamePlanException, SubscriptionCancelledException, SubscriptionPayment (inmutable),
                                 PaymentDraft (validación) y PaymentTerms, PaymentMethod, InvalidPaymentException,
                                 SubscriptionHistoryEntry (PLAN o STATUS)
  domain/event/                  SubscriptionEvent, SubscriptionStatusChanged, SubscriptionPlanChanged
  application/                   ChangeComplexPlan, ChangeSubscriptionStatus, RecordComplexPayment,
                                 ListComplexPayments, ListComplexHistory, ComplexLocks y LockedComplex
                                 (concurrencia optimista), RecordedPayment, HistoryItem
  application/port/out/          SubscriptionHistory, SubscriptionPayments, SubscriptionEvents;
                                 Subscriptions suma lockByTenant y update
  infrastructure/persistence/global/  JpaSubscriptionHistory, JpaSubscriptionPayments
  infrastructure/events/         SpringSubscriptionEvents
  infrastructure/web/            SubscriptionController (/complexes/{id}/plan, /status-changes, /payments, /history),
                                 SubscriptionProblemHandler, SubscriptionResponses, PageParameters
```

En la fila «Tenancy», agregar el puerto público nuevo:

- `TenantProfileLock` (`application/port/in`, implementado por `JdbcTenantDirectory`): bloquea la fila de `tenant` de un complejo dentro de la transacción global de quien llama (`SELECT … FOR UPDATE`). Solo lee el directorio global.

En «Identidad y acceso», el puerto `PlatformUserNames` suma `displayNamesOf(ids)`: nombres de varios usuarios de plataforma en una consulta (sin N+1). Lo implementa `JpaPlatformUserProfiles`.

Decisiones de implementación:

- **Concurrencia optimista de un complejo.** Toda mutación bloquea primero la fila de `subscription` y después la de `tenant` (siempre en ese orden, para no provocar interbloqueos) y compara `tenant.version + subscription.version` con las dos filas bloqueadas. Si no coincide: `409 STALE_STATE`. PLATODO-86 debe usar `ComplexLocks` (o el mismo orden) en el cambio de plazo y en el efecto sobre el tenant.
- **`SubscriptionStatusChanged` es síncrono.** Se publica dentro de la transacción del cambio; si un consumidor síncrono (`@EventListener`) falla, no queda nada del cambio (prueba `aFailingSynchronousConsumerRollsTheStatusChangeBack`). La respuesta se lee después de los consumidores, así que ya muestra su efecto. Hoy nadie lo escucha: el efecto sobre el tenant (SUSPENDED, ARCHIVED, plazo) lo agrega PLATODO-86.
- **`SubscriptionPlanChanged`** se publica en la transacción del cambio; sus consumidores deben escuchar después del commit. Hoy nadie lo escucha.
- **Ningún proceso cambia estados.** La prueba de arquitectura `SubscriptionStatusChangesArchitectureTest` exige que solo `ChangeSubscriptionStatus` y `ChangeComplexPlan` llamen a `Subscriptions.update`, y que solo los alcance su controlador.
- **Historial.** El alta escribe dos entradas con `before` nulo: `PLAN` (nombre del plan) y `STATUS` (`ACTIVE`). Cada cambio de plan o estado agrega una con el valor anterior, el nuevo, la fecha y el responsable. El listado nunca devuelve entradas de más de 365 días, aunque el borrado programado sea de PLATODO-87. Los complejos dados de alta antes de V9 no tienen las entradas del alta.
- **Pagos.** Inmutables (sin operaciones de edición ni borrado; columnas `updatable = false`). La moneda sale de la instantánea del plan de la suscripción, que se bloquea al registrar para que un cambio de plan simultáneo no la cambie. Registrar un pago no cambia la `version` del complejo.
- **«Hoy» para `paidOn`** es la fecha UTC. [POR DEFINIR: zona horaria del país de lanzamiento]. En América la fecha UTC nunca va detrás de la local, así que un pago de hoy no se rechaza.
- **Migración global V9:** tabla `subscription_history` con índices por complejo y fecha, y por fecha (para el borrado de PLATODO-87). Solo crea una tabla: se aplica sobre datos reales sin cambios.
- **`PageParameters`** reúne la validación de `page` y `size` que repetían `ComplexController` y `PlanController`.

## docs/glossary.md

Agregar:

- **Historial de la suscripción:** cambios de plan y de estado de un complejo, con el valor anterior, el nuevo, la fecha y quién lo hizo. Se conserva 1 año.
- **Pago de suscripción:** pago que el complejo hizo fuera de la plataforma y que Kuador registra a mano (monto, fecha y medio: transferencia o factura). No se edita ni se borra.

## contracts/platform-backoffice.openapi.yaml

Para que lo revise el arquitecto (en esta tarea no se cambió el contrato):

1. **`RETENTION_EXPIRED` todavía no se responde.** Mientras el tenant no acepta cambios (no aprovisionado, en purga o con el plazo vencido), `allowedTransitions` está vacío y cualquier cambio de estado responde `422 TRANSITION_NOT_ALLOWED`. PLATODO-86 debe distinguir la reactivación después del plazo con `RETENTION_EXPIRED`.
2. **Efecto sobre el tenant.** Hasta PLATODO-86, `POST /status-changes` cambia solo el estado comercial: `tenantStatus`, `retentionDays` y `purgeDueAt` de la respuesta no cambian.
3. **Orden de validación de `PUT /plan`:** campos (`400`) → `version` (`409 STALE_STATE`) → plan publicado (`PLAN_NOT_PUBLISHED`) → suscripción cancelada (`SUBSCRIPTION_CANCELLED`) → mismo plan (`SAME_PLAN`). El de `POST /status-changes`: campos → `version` → transición.
4. **`VALIDATION_FAILED` del pago:** `amount` `REQUIRED`, `INVALID_FORMAT` (fuera del patrón `Money`) o `NOT_POSITIVE`; `paidOn` `REQUIRED` o `DATE_IN_FUTURE`; `method` `REQUIRED` o `NOT_ALLOWED`. Un `paidOn` que no es una fecha responde `400 INVALID_REQUEST`. Propuesta: listarlo en `recordComplexPayment`.
5. **`targetStatus` desconocido** o `version` ausente o negativa responden `400 INVALID_REQUEST`.
6. **Historial del alta:** dos entradas con `before: null` (`PLAN` y `STATUS`), como ya permite la descripción de `HistoryEntry`. Propuesta: agregarlas al ejemplo de `listComplexHistory`.
