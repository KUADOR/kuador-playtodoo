# Cambios a documentos compartidos · PLATODO-55

La tarjeta pide documentar en AGENTS.md los comandos para levantar en local. Por la regla de documentos compartidos, el cambio queda aquí y se pasa a AGENTS.md en el release. Reemplaza también el bloque que propone `docs/changes/PLATODO-29.md` para «Levantar en local».

## AGENTS.md

En «Comandos del proyecto», reemplazar el bloque «Levantar en local» por:

```bash
# Levantar en local (detalle: infra/local/README.md)
docker compose -f infra/local/compose.yaml up -d --wait   # PostgreSQL 18 (5432) y proxy por subdominio (8000)
(cd api && ./mvnw spring-boot:run)                         # api en :8080 (JDK 25)
(cd app && pnpm dev --host 127.0.0.1)                      # app en :5173
(cd backoffice && npm start -- --host 127.0.0.1)           # backoffice en :4200
# Entrar: http://{subdominio}.localhost:8000 (complejo), http://api.localhost:8000 (api global),
# http://admin.localhost:8000 (backoffice)
docker compose -f infra/local/compose.yaml down            # detener (-v borra los datos)
infra/proxy/test/smoke.sh                                  # prueba de humo del proxy (Docker)
```

## docs/architecture.md

En ADR-002 (tabla de hosts), agregar debajo:

- El proxy inverso (Caddy, `infra/proxy/`) enruta por host y conserva el `Host` original; fija `X-Forwarded-Host`, `X-Forwarded-For` y `X-Forwarded-Proto` y reemplaza los que envía un cliente que no está en la lista de proxies de confianza del entorno. Rechaza con 404 los subdominios mal formados (ADR-012) o anidados sin llegar a la api.
- Certificado TLS comodín `*.playtodoo.com` (más `playtodoo.com`) por ACME DNS-01.
- La api confía en `X-Forwarded-*` solo desde la red del proxy (`server.tomcat.remoteip.internal-proxies`).

## README.md

En la sección de desarrollo local, enlazar `infra/local/README.md` como forma de levantar todo por subdominio.
