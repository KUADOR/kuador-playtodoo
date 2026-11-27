# Cambios a documentos compartidos · PLATODO-86

Suspensión, cancelación, reactivación y plazo de conservación en la `api` (ADR-014, ADR-019; PLATODO-27 CA8 y CA9).

## docs/architecture.md

En la sección «Módulos», fila «Tenancy», agregar:

```
com.kuador.platodo.api.tenancy/
  domain/                        Tenant.suspend(now, días) fija el plazo desde la suspensión; archive conserva el plazo
                                 de un suspendido; reactivate solo desde SUSPENDED antes de purgeDueAt;
                                 changeRetention en SUSPENDED o ARCHIVED; NotInRetentionException
  domain/event/                  TenantStatusChanged (síncrono, en la transacción del cambio)
  application/                   TenantLifecycle: suspend, cancel, reactivate (los dispara la suscripción) y
                                 changeRetention; TenantTransitions publica TenantStatusChanged en cada cambio de estado
  application/port/in/           TenantRetention (público): cambia el plazo dentro de la transacción de quien llama
  infrastructure/events/         SubscriptionStatusEffects: @EventListener síncrono de SubscriptionStatusChanged
```

En la fila «Planes y suscripciones», agregar:

- `ChangeComplexRetention` (`PUT /complexes/{complexId}/retention`, solo `SUPER_ADMIN`): bloquea el complejo con `ComplexLocks` (suscripción y después tenant) y llama a `TenantRetention`.
- `ChangeSubscriptionStatus` responde `422 RETENTION_EXPIRED` al reactivar una suscripción suspendida cuyo plazo venció o cuyo tenant está `PURGING` o `PURGED` (`RetentionExpiredException`).

En la fila «Reservas»: las respuestas de personal llevan `inactive`, calculado en la presentación (`BookingInactivity`) a partir del estado del tenant resuelto, con una consulta al directorio global por solicitud (no por entrada). No se escribe ninguna fila de `booking` (ADR-019).

En «Catálogo de eventos», agregar `TenantStatusChanged` (tenantId, status, purgeDueAt, occurredAt): lo publica Tenancy en cada cambio de estado del tenant, dentro de la misma transacción; sus consumidores escuchan con `@EventListener` y, si fallan, se revierten el tenant y la suscripción (prueba `aFailingTenantStatusConsumerRollsBackTheTenantAndTheSubscription`). `TenantActivated`, `TenantSuspended` y `TenantArchived` se siguen publicando como antes.

En «Ciclo de vida del tenant», precisar:

- **Ya no hay cambios directos del tenant.** Se quitaron `POST /platform/tenants/{tenantId}/suspend`, `/activate`, `/archive`, `/reactivate` y `PUT /platform/tenants/{tenantId}/retention` (ADR-014 §7): el estado del tenant solo cambia por la suscripción (`POST /complexes/{complexId}/status-changes`) y el plazo por `PUT /complexes/{complexId}/retention`.
- **Instante del efecto:** la transición del tenant usa el instante del cambio de suscripción, así `suspendedAt` coincide con `subscription.statusSince`.
- **Versión del complejo:** suspender, cancelar y reactivar suben la versión de la suscripción y la del tenant (`version` aumenta en 2); `EXPIRING` y el cambio de plan, en 1; cambiar el plazo, en 1 (solo el tenant).
- **Plazo por defecto:** sigue en la propiedad `tenancy.archive.default-retention-days` (45), que ahora vale también para la suspensión. El parámetro `retention.default-days` de `platform_parameter` que menciona esta sección no se sembró: ninguna pantalla lo cambia y no hacía falta otra fuente. Corregir el nombre en la línea «Plazo de conservación».

## docs/adr/ADR-019-reservas-inactivas-derivadas.md

Sin cambios de decisión. Nota de implementación para la sección «Consecuencias»: el estado se consulta con `TenantDirectory.find` (una consulta por solicitud); el filtro todavía no deja pasar a nadie a un complejo `SUSPENDED` o `ARCHIVED` (eso llega con la sesión de soporte de PLATODO-88), así que hoy `inactive` es siempre `false` en las respuestas reales y el caso `true` está probado en `BookingInactivityTest`.

## Catálogo global de complejos (para el arquitecto)

El diseño de la épica da por existente la tabla `complex_catalog` y el módulo «Complejos y canchas», pero en el repo no existen (no hay tabla, migración ni código del catálogo). Esta tarea publica `TenantStatusChanged` de forma síncrona y prueba que un consumidor que falla revierte todo, que es el punto de enganche del catálogo. El consumidor que oculta y muestra la fila queda para la tarea que cree el catálogo; mientras no exista, ningún complejo aparece en ningún catálogo y el bloqueo lo da el filtro del host.

## infra/local/e2e/tenancy-walkthrough.sh

El recorrido local de PLATODO-58 usa las operaciones directas del tenant que se quitaron (`suspend`, `activate`, `archive`, `reactivate`) y el alta deprecada sin suscripción. Hay que migrarlo a `contracts/platform-backoffice.openapi.yaml` (alta con plan y `status-changes`); propuesta: en la tarea de integración final de PLATODO-27.

## contracts/platform-backoffice.openapi.yaml

Sin cambios. Para que lo revise el arquitecto:

1. **Orden de validación de `PUT /complexes/{complexId}/retention`:** campos (`400`) → `retentionDays` (`422 VALIDATION_FAILED`, `NOT_POSITIVE` si es menor que 1, `NOT_ALLOWED` sobre el techo técnico de 36 500 días) → `version` (`409 STALE_STATE`) → estado (`NOT_IN_RETENTION`) → fecha (`RETENTION_DEADLINE_IN_PAST`).
2. **`RETENTION_EXPIRED`** se evalúa antes que `TRANSITION_NOT_ALLOWED` cuando se pide `ACTIVE` desde `SUSPENDED`.

## contracts/tenancy.openapi.yaml

Se quitaron las cinco operaciones de ciclo de vida `deprecated` y los componentes que solo usaban ellas (`ChangeRetentionRequest`, `TenantChanged`, `InvalidTransition`, ejemplos `ArchivedTenant`, `InvalidTransitionProblem`, `RetentionExpiredProblem` y los códigos `RETENTION_EXPIRED` y `RETENTION_INVALID`).
