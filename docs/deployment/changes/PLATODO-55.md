# Cambios de despliegue · PLATODO-55

DNS y TLS comodín, proxy inverso por subdominio y entorno local con `{sub}.localhost`.

## Infraestructura

- **Proxy inverso:** Caddy 2.11.9, único punto de entrada desde internet. Configuración en `infra/proxy/` (detalle en `infra/proxy/README.md`).
- **Certificados:** uno comodín `*.playtodoo.com` y uno para `playtodoo.com`, emitidos por ACME (Let's Encrypt) con el desafío **DNS-01** (el único que emite comodines). La imagen se construye con `infra/proxy/Dockerfile` y el módulo del proveedor DNS. [POR DEFINIR: proveedor DNS de `playtodoo.com`].
- **Almacenamiento:** el volumen `/data` del proxy guarda certificados y la cuenta ACME. Debe ser persistente en cada entorno.

## Piezas y dominios

| Host | Destino | Pruebas | Producción |
|---|---|---|---|
| `{sub}.<raíz>/api/**` | api (contexto de tenant) | [POR DEFINIR: dominio raíz de pruebas] | `*.playtodoo.com` |
| `{sub}.<raíz>/**` | app | idem | `*.playtodoo.com` |
| `api.<raíz>` | api (contexto global) | idem | `api.playtodoo.com` |
| `admin.<raíz>` | backoffice | idem | `admin.playtodoo.com` |
| `<raíz>`, `www.<raíz>` | 404 hasta que exista el sitio raíz [POR DEFINIR: sitio raíz] | idem | `playtodoo.com` |

Subdominios mal formados (formato de ADR-012) o anidados: 404 en el proxy. Subdominio bien formado sin complejo: 404 `TENANT_NOT_FOUND` de la api (PLATODO-50).

Puertos: el proxy publica 80 y 443 (y 443/UDP para HTTP/3). La api y las webs no se publican: solo el proxy las alcanza por la red interna.

## DNS (dependencia externa)

Registros en la zona de `playtodoo.com` (los crea una persona con acceso al proveedor DNS):

| Registro | Tipo | Apunta a |
|---|---|---|
| `playtodoo.com` | A / AAAA | IP pública del proxy de producción |
| `*.playtodoo.com` | A / AAAA | IP pública del proxy de producción |
| Pruebas | A / AAAA | Según el dominio raíz de pruebas [POR DEFINIR] |

Un comodín no cubre el apex: se necesitan los dos registros. Los nombres explícitos (`api`, `admin`) quedan cubiertos por el comodín.

## Variables y secretos

Solo nombres; los valores viven en la plataforma de despliegue [POR DEFINIR: plataforma].

| Nombre | Pieza | Entornos | Secreto |
|---|---|---|---|
| `PLATODO_ROOT_DOMAIN` | proxy | pruebas, producción | no |
| `PLATODO_API_UPSTREAM`, `PLATODO_APP_UPSTREAM`, `PLATODO_BACKOFFICE_UPSTREAM` | proxy | pruebas, producción | no |
| `PLATODO_TRUSTED_PROXIES` | proxy | pruebas, producción (vacío si Caddy es el borde) | no |
| `ACME_EMAIL` | proxy | pruebas, producción | no |
| `ACME_DNS_PROVIDER` | proxy | pruebas, producción | no |
| `ACME_DNS_API_TOKEN` | proxy | pruebas, producción | **sí** (token del proveedor DNS limitado a editar registros TXT de la zona) |
| `SERVER_REQUEST_HEADER_FORWARDED_STRATEGY` = `web-server` | api | pruebas, producción | no |
| `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` | api | pruebas, producción (solo la red del proxy) | no |

`PLATODO_ROOT_DOMAIN` del proxy debe coincidir con `playtodoo.root-domain` de la api (ADR-012).

## CI/CD

| Workflow | Se activa con | Qué hace |
|---|---|---|
| `.github/workflows/proxy.yml` | PR o push a develop/main que toque `infra/` | Formato de los Caddyfile, validación de los compose y prueba de humo de enrutamiento (`infra/proxy/test/smoke.sh`) |

Sin secretos en CI. No agregar `proxy` como check obligatorio (está filtrado por ruta).

## Qué hacer al desplegar

- **Local:** `docker compose -f infra/local/compose.yaml up -d --wait` (ver `infra/local/README.md`).
- **Pruebas (develop), primero:**
  1. Definir dominio raíz y proveedor DNS; crear el token DNS limitado a la zona y cargarlo como secreto.
  2. Construir la imagen: `docker build --build-arg DNS_PROVIDER_MODULE=github.com/caddy-dns/<proveedor> -t platodo-proxy infra/proxy`.
  3. Crear los registros DNS de pruebas; desplegar el proxy con `/data` persistente y las variables.
  4. Configurar en la api las dos variables de proxies de confianza.
  5. Verificar: `curl -I https://api.<raíz>`, `curl -I https://<sub>.<raíz>/api/v1/tenant` y `openssl s_client -connect <sub>.<raíz>:443 -servername <sub>.<raíz> </dev/null | openssl x509 -noout -subject -issuer` (sujeto `*.<raíz>`).
- **Producción (main):** mismos pasos con `playtodoo.com`, **solo con aprobación de una persona** y después de verificarlo en pruebas. Este PR no toca producción.

## Vuelta atrás

- Proxy: volver a la imagen anterior; los certificados siguen en `/data`.
- DNS: restaurar los registros anteriores (bajar el TTL antes del cambio para acortar la propagación).
- CI: borrar `.github/workflows/proxy.yml` en un PR a develop.
