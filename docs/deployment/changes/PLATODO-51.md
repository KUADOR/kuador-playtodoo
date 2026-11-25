# Cambios de despliegue · PLATODO-51

Identidad y acceso en la api: personal por complejo, jugador y Kuador globales, tokens JWT firmados por la api.

## Variables

| Nombre | Pieza | Entornos | Secreto | Valor |
|---|---|---|---|---|
| `PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY` (propiedad `playtodoo.identity.token.signing-key`) | api | local, pruebas, producción | **sí** | Clave HMAC-SHA256 en Base64, de al menos 256 bits (por ejemplo `openssl rand -base64 32`). Una distinta por entorno; nunca en el repo. **Sin ella la api no arranca** |
| `PLAYTODOO_IDENTITY_TOKEN_TIME_TO_LIVE` (propiedad `playtodoo.identity.token.time-to-live`) | api | opcional | no | Duración ISO-8601 de un token. Por defecto `PT1H` |

- Cambiar la clave invalida todos los tokens emitidos: las personas vuelven a iniciar sesión. Rotarla así si se filtra.
- En local, exportar la variable antes de `./mvnw spring-boot:run` (por ejemplo `export PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY=$(openssl rand -base64 32)`). Las pruebas usan una clave propia de pruebas (`api/src/test/resources/config/application.properties`), que no sirve en ningún entorno.

## Dependencias

- `spring-security-oauth2-jose` (Nimbus JOSE) y `spring-security-crypto` (BCrypt), versiones del BOM de Spring Boot 4.2.0. No se agrega `spring-security-web`: no cambia ningún filtro de seguridad por defecto.

## Base de datos

- Migración global `V3__create_player_account_and_platform_user.sql`: tablas `global.player_account` y `global.platform_user`.
- Migración de tenant `V2__create_staff_user.sql`: tabla `staff_user` en cada esquema `t_<uuid>`. Se aplica sola al arrancar a cada tenant `ACTIVE`, `SUSPENDED` o `ARCHIVED`.
- No hay cuentas iniciales: el alta de cuentas llega con sus tareas (provisión del propietario en PLATODO-52, registro de jugadores y gestión de personal en sus épicas). Hasta entonces, crear un usuario de Kuador en un entorno de pruebas exige un `INSERT` manual con el hash BCrypt (`{bcrypt}…`) de la contraseña; nunca en producción sin una decisión aprobada.

## Qué hacer al desplegar

- **Pruebas y producción:** crear el secreto `PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY` antes de desplegar esta versión. Verificar con `curl -i -X POST https://api.<raíz>/api/v1/auth/players/login -H 'Content-Type: application/json' -d '{"email":"nadie@example.com","password":"x"}'` → 401 `INVALID_CREDENTIALS`.
- **Vuelta atrás:** la versión anterior ignora las tablas nuevas; no hace falta revertir las migraciones.
