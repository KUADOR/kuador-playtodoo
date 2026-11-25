# Cambios a documentos compartidos · PLATODO-56

Notas para pasar a los documentos del proyecto al preparar el release (regla de documentos compartidos).

## app/README.md

En «Structure», agregar:

| Path | Content |
|---|---|
| `src/tenant/` | Tenant gate: `GET /tenant` port, HTTP adapter and in-memory mock; «not available», «not found», loading and network-error screens |
| `src/dev/tenant-state/` | Tenant states on the api mock at `/dev/tenant-state?state=…`, only in development builds |

Y en `src/shared/ui/`, sumar `ButtonLink` (enlace con el aspecto de `Button`).

## docs/architecture.md

En «Clientes», fila `app`: la portada del complejo queda detrás de `TenantGate`, que consulta `GET /api/v1/tenant` y muestra la app solo si el complejo está activo (`403 TENANT_UNAVAILABLE` → «Complejo no disponible», `404 TENANT_NOT_FOUND` → «Complejo no encontrado», cualquier otra falla → «No pudimos comprobar el complejo» con Reintentar).

## DESIGN.md

- Nuevo componente base `ButtonLink`: mismas variantes y medidas que `Button`, para navegaciones.
- [POR DEFINIR: token de ancho de las tarjetas de estado]. Hoy 480 px, según Pantallas PLATODO-26.
- [POR DEFINIR: logo]. Las pantallas de estado muestran el nombre «Playtodoo» en la tipografía de títulos mientras no haya logo.
