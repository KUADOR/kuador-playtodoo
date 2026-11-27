# Cambios de despliegue · PLATODO-95

Manual del ambiente de pruebas en Dokploy: `docs/despliegue-dokploy-develop.md`. Solo documentación; no cambia
código, workflows ni ningún entorno. Todavía sin validar en un VPS (PLATODO-96).

## Qué define para «Pruebas»

| Sección de «Despliegue» | Qué pasar al preparar el release |
|---|---|
| Entornos | Pruebas: rama `develop`, Dokploy en un VPS, raíz `develop.play.kuador.com` |
| Infraestructura | Borde: Traefik de Dokploy con Let's Encrypt por HTTP. El proxy Caddy de `infra/proxy` no se usa en pruebas |
| Piezas | `api` (Railpack, JDK 25, puerto 8080), `app` y `backoffice` (Nixpacks, estáticos, puerto 80); hosts de la sección 2 del manual |
| Dependencias | PostgreSQL `postgres:18-alpine` como servicio de base de datos de Dokploy, sin puerto público |
| Variables y secretos | Inventario de la sección 4 del manual. Nombres nuevos, solo de build: `RAILPACK_JDK_VERSION` (api) y `NIXPACKS_NODE_VERSION` (app, backoffice) |
| CI/CD | Auto-despliegue por la GitHub App de Dokploy en cada push a `develop`, filtrado por `Watch Paths`. Sin secretos en GitHub |
| Verificación | Lista de la sección 10 del manual |

- En pruebas, `ACME_*` y `PLATODO_*` del proxy (PLATODO-55) no aplican: son del proxy Caddy.
- Cada complejo de pruebas necesita dos dominios en Dokploy (sección 5.6 del manual).

## Qué hacer al desplegar

- **Pruebas (develop):** seguir el manual. Requiere los registros DNS de `*.develop.play.kuador.com` (los crea el
  equipo).
- **Producción (main):** nada. Queda fuera de la épica.

## Vuelta atrás

Borrar el proyecto en Dokploy y los registros DNS. No hay estado fuera del VPS.
