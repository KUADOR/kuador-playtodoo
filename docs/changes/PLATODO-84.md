# Cambios a documentos compartidos · PLATODO-84

## docs/architecture.md

En la sección «Módulos», fila «Planes y suscripciones», agregar lo que suma PLATODO-84 (ADR-014, ADR-018):

```
com.kuador.platodo.api.subscriptions/
  domain/                        Subscription, SubscriptionStatus (transiciones manuales), PlanSnapshot,
                                 ComplexRegistration (validación del alta), LastPayment, PlanNotPublishedException,
                                 InvalidComplexRegistrationException
  application/                   RegisterComplex, RetryComplexProvisioning, GetComplex, ListComplexes,
                                 ComplexDetails (modelo de lectura del detalle), ComplexDetail, ComplexSummary
  application/port/out/          Subscriptions, LastPayments
  infrastructure/persistence/global/  JpaSubscriptions, JpaLastPayments, PlanSnapshotJson
  infrastructure/web/            ComplexController (/complexes, /complexes/{id}, /complexes/{id}/provisioning-retries),
                                 ComplexesProblemHandler, ComplexResponses, MoneyText
```

En la fila «Tenancy», agregar los puertos públicos nuevos:

- `TenantOnboarding` (`application/port/in`, implementado por `OnboardTenant`): `record` registra el tenant en `PROVISIONING` dentro de la transacción global de quien llama; `provision` crea y migra el esquema y la cuenta del propietario y deja el tenant `ACTIVE` o `PROVISIONING_FAILED`; `retryProvisioning` vuelve de `PROVISIONING_FAILED` a `PROVISIONING` y reintenta. No autoriza: lo hace el módulo que lo llama (`CapabilityGuard`). Errores propios: `SubdomainUnavailableException` (`INVALID`, `RESERVED`, `TAKEN`) y `ProvisioningNotFailedException`.
- `TenantDirectory.profiles(ids)`: datos del registro global de varios complejos en una consulta (sin N+1).
- Estado nuevo `PROVISIONING_FAILED`: el host del complejo responde «no encontrado» y la migración al arrancar no lo toma (lo migra el reintento).

En «Identidad y acceso», agregar el puerto público `PlatformUserNames` (nombre visible de un usuario de plataforma), que usa el detalle para `statusChangedBy`.

Decisiones de implementación:

- **Alta en una transacción global:** plan publicado → tenant y subdominio → suscripción `ACTIVE` con instantánea del plan. Si el subdominio o el plan se rechazan no queda nada. Dos altas simultáneas con el mismo subdominio: la clave primaria de `tenant_domain` deja pasar solo una; la otra responde `409 SUBDOMAIN_TAKEN`.
- **Aprovisionamiento después del commit**, fuera de la transacción. Si falla, se registra en el log y el tenant queda `PROVISIONING_FAILED` (respuesta `201` igual). Excepción E1: solo se crea el esquema que deriva del id nuevo; la prueba `registeringAComplexDoesNotTouchTheSchemaOfAnother` lo demuestra.
- **`version` del complejo** = `tenant.version + subscription.version`. Las dos solo crecen, así que cualquier cambio en cualquiera cambia la suma. PLATODO-85 debe compararla con las dos filas bloqueadas.
- **Último pago:** el de `paidOn` más reciente; a igual fecha, el último registrado. Se lee en una consulta por página.
- **Migración global V8:** `tenant.status` admite `PROVISIONING_FAILED` (la columna pasa a `varchar(20)`); `tenant.owner_email` (dato personal, la purga lo redacta, ADR-016); tablas `subscription` y `subscription_payment` (esta la escribe PLATODO-85). Se aplica sobre datos reales sin cambios: las columnas nuevas admiten nulos.
- **Fuga de conexiones corregida:** `JdbcSubdomainUsage` usaba `JdbcClient...query().stream()` sin cerrar el stream, y cada consulta de disponibilidad o sugerencia retenía una conexión del pool. Ahora lee con `list()`. Las pruebas nuevas, que consultan la disponibilidad varias veces, agotaban el pool.
- `/platform/tenants` (`tenancy.openapi.yaml`) también guarda ahora `owner_email`, y la purga lo redacta.

## docs/glossary.md

Agregar:

- **Instantánea del plan:** copia de las condiciones de un plan publicado (nombre, precio, moneda, periodicidad, límites y campos adicionales) que se guarda en la suscripción al asignarlo. Editar el plan después no la cambia.
- **Alta incompleta (`PROVISIONING_FAILED`):** complejo registrado con su subdominio y su suscripción cuyo esquema no se pudo crear. No es accesible y espera un reintento desde el backoffice.

## contracts/platform-backoffice.openapi.yaml

Para que lo revise el arquitecto (en esta tarea no se cambió el contrato):

1. **Reintento síncrono.** `POST /complexes/{id}/provisioning-retries` hace el reintento antes de responder: el `202` ya trae `tenantStatus` `ACTIVE` o `PROVISIONING_FAILED`, nunca `PROVISIONING`. Polling no hace falta, aunque funciona. Propuesta: decirlo en la descripción y cambiar el ejemplo `ProvisioningComplex`.
2. **Complejos sin suscripción.** Los registrados por el `POST /platform/tenants` heredado (`tenancy.openapi.yaml`) no tienen suscripción: no aparecen en `GET /complexes` y su detalle responde `404 COMPLEX_NOT_FOUND`. Propuesta: anotarlo o retirar esa operación heredada.
3. **`VALIDATION_FAILED` del alta:** `displayName` `REQUIRED` o `TOO_LONG`; `ownerEmail` `REQUIRED`, `TOO_LONG` o `INVALID_FORMAT`. Si faltan `subdomain` o `planId`, o `planId` no es un UUID, la respuesta es `400 INVALID_REQUEST`. Un `complexId` que no es UUID también responde `400`. Propuesta: listarlo en `registerComplex` y `getComplex`.
4. **Orden de validación del alta:** campos → plan (`PLAN_NOT_PUBLISHED`) → subdominio (`SUBDOMAIN_*`).
5. **`retentionDays` y `purgeDueAt`** se devuelven solo en `SUSPENDED` y `ARCHIVED`, como dice el contrato. Después de la purga son `null`, aunque el registro conserve los datos del archivo.
6. **`statusChangedBy` al dar de alta:** es quien registró el complejo, y `statusSince` es la fecha del alta.
