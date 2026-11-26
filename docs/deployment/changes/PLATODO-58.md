# Cambios de despliegue · PLATODO-58

## movil: host de los complejos

Nueva variable de compilación de `movil`: `PLAYTODOO_TENANT_URL_PATTERN` (`--dart-define`). Es el host web de un complejo con `{subdomain}`, el mismo valor que `PLAYTODOO_TENANT_URL_PATTERN` de la api. No es un secreto.

| Entorno | Valor |
|---|---|
| Local | `http://{subdomain}.localhost:8000` |
| Producción | `https://{subdomain}.playtodoo.com` (valor por defecto si no se pasa) |
| Otros entornos | [POR DEFINIR: dominio raíz de cada entorno desplegado] |

Al compilar `movil` para un entorno distinto de producción, pasar `--dart-define=PLAYTODOO_TENANT_URL_PATTERN=<valor>`.

## Herramientas de verificación local

`infra/local/e2e/` (recorrido de extremo a extremo) solo se usa en local: no cambia ningún entorno desplegado.

## proxy: subdominios mal formados

`infra/proxy/routes.caddy` ya no filtra el formato del subdominio: todo host de una sola etiqueta bajo el dominio raíz (salvo `api`, `admin`, `www` y el raíz) llega a la api (`/api/**`) o a la app (resto). La api valida el formato de ADR-012 antes de buscar el complejo y responde `404 TENANT_NOT_FOUND`, como pide el contrato, y la app y movil muestran «Complejo no encontrado». Los hosts con más de una etiqueta y los dominios ajenos siguen recibiendo `404` del proxy.

Al desplegar en cada entorno: reconstruir o recargar el proxy con la configuración nueva. Sin variables, secretos ni puertos nuevos.
