# Cambios de despliegue · PLATODO-53

Sesiones de acceso de Kuador auditadas (ADR-011): el dueño de Kuador abre una sesión con motivo y cada solicitud suya en el host de un complejo se audita antes de ejecutarse.

## Variables

| Nombre | Pieza | Entornos | Secreto | Valor |
|---|---|---|---|---|
| `PLAYTODOO_CORS_BACKOFFICE_ORIGIN` (propiedad `playtodoo.cors.backoffice-origin`) | api | pruebas, producción | no | Único origen de navegador admitido por CORS en la api, además del propio host. Producción: `https://admin.playtodoo.com`. Por defecto `http://admin.localhost:8000` (entorno local de `infra/local`) |
| `PLAYTODOO_AUDIT_ACCESS_SESSION_TIME_TO_LIVE` (propiedad `playtodoo.audit.access-session.time-to-live`) | api | opcional | no | Duración ISO-8601 de una sesión de acceso. Por defecto `PT30M`. Es un parámetro de seguridad, no de negocio |

## Base de datos

- Migración global `V4__create_access_session_and_access_audit_event.sql`: tablas `global.access_session` y `global.access_audit_event` (módulo Auditoría), con FK a `tenant` y `platform_user`. Se aplica sola al arrancar.

## Qué hacer al desplegar

- **Pruebas y producción:** definir `PLAYTODOO_CORS_BACKOFFICE_ORIGIN` con el origen exacto del backoffice de ese entorno (esquema, host y puerto). Sin ella se usa el valor local y el backoffice desplegado no podrá llamar a la api desde el navegador.
- **Verificar:** `curl -i -X OPTIONS https://<subdominio>.<raíz>/api/v1/tenant -H 'Origin: https://admin.<raíz>' -H 'Access-Control-Request-Method: GET'` → 200 con `Access-Control-Allow-Origin`; con otro `Origin` → 403.
- **Vuelta atrás:** la versión anterior ignora las tablas nuevas; no hace falta revertir la migración. Un token `PLATFORM` vuelve a recibir 403 en los hosts de complejo.
