# Cambios de despliegue · PLATODO-56

## Qué cambia

- **Nueva variable de compilación de `app` (opcional):** `VITE_PLATFORM_URL`, URL raíz de Playtodoo en el entorno. Es el destino del botón «Ir a Playtodoo» de las pantallas «Complejo no disponible» y «Complejo no encontrado». Si no se define, `app` usa el host actual sin su primer nivel (`elsol.playtodoo.com` → `https://playtodoo.com/`, `elsol.localhost:5173` → `http://localhost:5173/`).
- Al cargar, `app` llama a `GET /api/v1/tenant` en su mismo origen (sin credenciales). El proxy del subdominio del complejo (PLATODO-55) debe enrutar `/api/v1/*` a la `api`.

## Qué hacer al desplegar

| Entorno | Acción |
|---|---|
| Local | Nada obligatorio. Sin `api` levantada la portada muestra «No pudimos comprobar el complejo». Para ver los estados sin `api`: `pnpm dev` y abrir `/dev/tenant-state?state=unavailable\|not-found\|network-error\|active`. |
| CI | Nada. |
| Entornos reales | Definir `VITE_PLATFORM_URL` solo si la raíz no es el host del complejo sin su subdominio. Es pública (no es un secreto). |

Sin secretos, servicios ni migraciones nuevas.
