# Cambios a documentos compartidos · PLATODO-82

## docs/architecture.md

En la sección «Identidad y acceso», agregar lo que suma PLATODO-82 (ADR-017):

```
com.kuador.platodo.api.identity/
  domain/                        PlatformRole (SUPER_ADMIN, OPERATOR), Capability (enum de capacidades delegables),
                                 OperatorCapabilitySettings, CapabilityToggle, event/OperatorCapabilitiesChanged
  application/                   GetCurrentPlatformUser, ListOperatorCapabilities, ChangeOperatorCapabilities,
                                 ListCapabilityChanges
  application/port/in/           CapabilityGuard (puerto público para todo módulo con operaciones de plataforma),
                                 PlatformCaller y sus excepciones (401, 403 FORBIDDEN, 403 CAPABILITY_REVOKED)
  application/port/out/          OperatorCapabilities, OperatorCapabilityUpdates, CapabilityChangeLog,
                                 PlatformUserProfiles, OperatorCapabilityEvents
  infrastructure/persistence/global/  JpaOperatorCapabilities, JpaCapabilityChangeLog, JpaPlatformUserProfiles
  infrastructure/web/            PlatformIdentityController (GET /me, GET y PUT /operator-capabilities,
                                 GET /operator-capabilities/changes), CapabilityGuardProblemHandler (global)
```

- **Cómo protege un módulo una operación de plataforma:** llama a `CapabilityGuard` al empezar el caso de uso: `requirePlatformUser()` (lectura de ambos roles), `requireSuperAdmin()` (solo SUPER_ADMIN) o `require(Capability.X)` (SUPER_ADMIN, u OPERATOR con X habilitada). Las denegaciones se responden igual en toda la api por `CapabilityGuardProblemHandler`.
- **Por solicitud:** el guard lee `operator_capability` en cada llamada (una consulta, sin caché); el rol viaja en el token.
- **Versión del conjunto:** la de `GET /operator-capabilities` es la mayor `version` de `operator_capability`. `PUT` bloquea todas las filas en orden de clave (`PESSIMISTIC_WRITE`), compara la versión y registra solo los cambios efectivos en `capability_change`; un lote sin cambios no sube la versión.
- **Capacidad nueva:** valor nuevo en `Capability`, el guard donde aplique y una migración global que la siembre en `platform_capability` y apagada en `operator_capability`.
- **Migración global V6:** `PLATFORM_OWNER` pasa a `SUPER_ADMIN`; `platform_user` suma `display_name` (las filas existentes toman la parte local del correo); tablas `platform_capability`, `operator_capability` y `capability_change`.
- Las operaciones de plataforma de PLATODO-26 (`/platform/tenants/**`, sesiones de acceso y su auditoría) siguen siendo solo del SUPER_ADMIN; abrirlas al OPERATOR con `TENANT_DATA_ACCESS` le toca a PLATODO-88.

## contracts/platform-backoffice.openapi.yaml

Para que lo revise el arquitecto (no se cambió el contrato en esta tarea): la descripción dice que un token `STAFF` recibe `403 FORBIDDEN` en todas las operaciones. La api responde `401 UNAUTHENTICATED` en el host global, porque desde PLATODO-26 (`contracts/tenancy.openapi.yaml`) un token de personal solo se acepta en el host de su propio complejo y ahí las rutas de plataforma responden `404`. En cualquiera de los dos casos el personal no accede (CA18). Los jugadores sí reciben `403 FORBIDDEN`. Propuesta: ajustar la frase del contrato a «`STAFF` tokens are rejected on the global host (`401 UNAUTHENTICATED`) and `PLAYER` tokens answer `403 FORBIDDEN`».
