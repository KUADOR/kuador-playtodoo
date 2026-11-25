# Cambios de despliegue · PLATODO-50

Resolución de tenant por subdominio en la api.

## Variables

| Nombre | Pieza | Entornos | Secreto | Valor |
|---|---|---|---|---|
| `PLAYTODOO_ROOT_DOMAIN` (propiedad `playtodoo.root-domain`) | api | pruebas, producción | no | Dominio raíz del entorno; producción `playtodoo.com`. Debe coincidir con `PLATODO_ROOT_DOMAIN` del proxy (PLATODO-55). Por defecto `localhost` (entorno local) |

- `server.request.header.forwarded.strategy=web-server` queda fijado en `application.properties`; la variable `SERVER_REQUEST_HEADER_FORWARDED_STRATEGY` de PLATODO-55 ya no es necesaria (no hace daño si se deja).
- `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` (PLATODO-55) sigue siendo obligatoria en pruebas y producción: limitada a la red del proxy. Sin ella, Tomcat confía en todas las redes privadas.

## Base de datos

- Migración global `V2__create_tenant_domain.sql` (tabla `global.tenant_domain`). Se aplica sola al arrancar; no necesita pasos manuales.

## Qué hacer al desplegar

- **Pruebas y producción:** definir `PLAYTODOO_ROOT_DOMAIN` antes de desplegar esta versión. Verificar con `curl -i https://<sub>.<raíz>/api/v1/tenant` (200 para un complejo activo, 404 `TENANT_NOT_FOUND` para un subdominio inexistente) y `curl -i https://api.<raíz>/api/v1/tenant` (404 `RESOURCE_NOT_FOUND`).
- **Vuelta atrás:** la versión anterior ignora la tabla nueva; no hace falta revertir la migración.
