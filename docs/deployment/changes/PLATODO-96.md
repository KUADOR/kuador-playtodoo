# Cambios de despliegue · PLATODO-96

Ensayo local del manual `docs/despliegue-dokploy-develop.md` (evidencia en
`odd/tasks/platodo-94-despliegue-vps-dokploy.md`). Solo documentación; no cambia código, workflows ni ningún entorno.
**El manual sigue sin validarse en un VPS.**

## Qué corrige sobre la nota de PLATODO-95

| Sección de «Despliegue» | Qué pasar al preparar el release |
|---|---|
| Piezas | `app` y `backoffice` se construyen con **Railpack**, no con Nixpacks; la imagen sirve el sitio con Caddy en el puerto 80 |
| Variables y secretos | Solo de build, sin secretos: `RAILPACK_BUILD_CMD` (api), `RAILPACK_NODE_VERSION` (app, backoffice) y `RAILPACK_SPA_OUTPUT_DIR` (backoffice). `NIXPACKS_NODE_VERSION` ya no se usa |

## Qué hacer al desplegar

- **Pruebas (develop):** seguir el manual. Sigue pendiente crear los registros DNS de `*.develop.play.kuador.com`.
- **Producción (main):** nada.

## Vuelta atrás

No aplica: no hay nada desplegado.
