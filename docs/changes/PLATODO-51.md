# Cambios a documentos compartidos · PLATODO-51

## docs/architecture.md

Agregar una sección «Identidad y acceso»:

```
com.kuador.platodo.api.identity/
  domain/                        AccountType (STAFF, PLAYER, PLATFORM), EmailAddress (normalizado)
  application/                   LogIn (una instancia por tipo de cuenta), AuthenticateRequest (reglas de aceptación
                                 del token por host), RequestAuthentication, InvalidCredentialsException
  application/port/in/           AuthenticatedPrincipal, CurrentPrincipal (ScopedValue de la solicitud)
  application/port/out/          AccessTokens, AccountDirectory, PasswordHashes
  infrastructure/token/          JwtAccessTokens (HS256, Nimbus), AccessTokenProperties
  infrastructure/crypto/         DelegatingPasswordHashes (BCrypt con prefijo de algoritmo)
  infrastructure/persistence/global/  JpaPlayerAccounts, JpaPlatformUsers (unidad global)
  infrastructure/persistence/tenant/  JpaStaffUsers (unidad de tenant)
  infrastructure/web/            AccessTokenFilter, LoginController, IdentityConfiguration, problem handlers
```

- **Dónde vive cada cuenta:** `staff_user` en el esquema de cada complejo (migración de tenant `V2`); `player_account` y `platform_user` en `global` (migración global `V3`). Las tablas operativas guardan solo el id del jugador, sin FK entre esquemas.
- **Token:** JWT HS256 firmado por la api con la clave del entorno. Claims `sub`, `typ`, `tid` (solo `STAFF`), `role`, `iat`, `exp`. Vida por defecto 1 h.
- **AccessTokenFilter** (orden `HIGHEST_PRECEDENCE + 20`, justo después de `TenantResolutionFilter`): sin `Authorization` la solicitud sigue anónima; con un token, `AuthenticateRequest` decide:
  - `STAFF`: solo en el host de su complejo (`tid` = tenant resuelto) y con la cuenta activa en ese esquema. En otro complejo o en el host global, 401 `UNAUTHENTICATED`.
  - `PLAYER`: en cualquier host, con la cuenta global activa (se consulta `global.player_account` en cada solicitud).
  - `PLATFORM`: en el host global con la cuenta activa. En el host de un complejo, 403 `ACCESS_SESSION_INVALID` hasta que PLATODO-53 agregue las sesiones de acceso.
  - Token inválido, vencido o con otro esquema que `Bearer`: 401 `UNAUTHENTICATED`.
- El principal aceptado queda en `CurrentPrincipal` (ScopedValue, no se hereda entre hilos). Reservas lo lee con `PrincipalCurrentPlayer` y `PrincipalCurrentStaff` (`bookings/infrastructure/identity`), que reemplazan a `UnauthenticatedCurrentPlayer` y `UnauthenticatedCurrentStaff`.
- **Login:** `POST /api/v1/auth/staff/login` (host del complejo), `/auth/players/login` y `/auth/platform/login` (host global). Cuenta inexistente, inactiva o contraseña errónea responden igual (401 `INVALID_CREDENTIALS`); con una cuenta inexistente igual se gasta una verificación BCrypt para no revelar por tiempo si el correo existe.
- Pruebas: `support/TestAccounts` crea cuentas directo en la base; `identity/.../web/TwoComplexesWithBookingApis` es el contexto compartido de las ITs de identidad (un solo contexto y un solo pool: la suite ya está cerca del límite de conexiones del PostgreSQL de Testcontainers). Las pruebas toman su clave de firma de `src/test/resources/config/application.properties`.

## api/README.md

- `./mvnw spring-boot:run` necesita además `PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY` (Base64, ≥ 256 bits; por ejemplo `export PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY=$(openssl rand -base64 32)`). Sin ella la api no arranca.

## Para las tareas siguientes de la épica PLATODO-26

- **PLATODO-52 (provisión):** crear el `staff_user` propietario con rol `OWNER` y el correo en minúsculas (`CHECK email = lower(btrim(email))`).
- **PLATODO-53 (sesiones de acceso):** reemplazar la regla `PLATFORM` en host de tenant de `AuthenticateRequest` por la validación de `X-Platform-Access-Session` y la auditoría.
- **Contrato:** un token `STAFF` en el host global responde 401 `UNAUTHENTICATED` (falla cerrada: la cuenta solo se puede validar en su complejo). Al agregar operaciones de plataforma, documentarlo junto al 403 `FORBIDDEN` de `contracts/tenancy.openapi.yaml`.
