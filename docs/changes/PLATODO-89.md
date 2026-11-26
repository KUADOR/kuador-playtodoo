# PLATODO-89 · Cambios para los documentos compartidos

Backoffice: base, inicio de sesión (P1) y permisos del operador (P13). Al preparar el release, pasar estas notas a sus documentos y borrar este archivo.

## docs/architecture.md

En la tabla de piezas, fila `backoffice`, reemplazar «[POR DEFINIR: organización de carpetas, gestión de estado y acceso a datos]» por:

> `src/app/core` (api, sesión, guardas, formato), `src/app/layout` (shell con navegación), `src/app/features/<sección>` (pantallas y su puerto a la api) y `src/app/shared/ui` (componentes de la Identidad visual). Estado local con signals de Angular; sin librería de estado global. Acceso a datos con `HttpClient` tipado a mano desde `contracts/*.openapi.yaml` (un servicio por grupo de operaciones) e interceptores comunes: token de plataforma, errores RFC 9457 (`ApiError` con `code`), 401 que cierra la sesión y 403 `CAPABILITY_REVOKED` que recarga `GET /me`. El backoffice en `admin.<raíz>` llama a la api en `api.<raíz>/api/v1`; en otro host, al mismo origen.

Agregar en «Decisiones» o en la sección de la pieza:

- Permisos en pantalla: el menú y las rutas usan las mismas reglas (`AccessRule`) leídas de `GET /me`; lo no permitido se oculta y una dirección directa muestra «No tienes permiso para ver esta sección.». La api decide (ADR-017).
- El token de plataforma vive en `sessionStorage` de la pestaña (no en cookie ni `localStorage`): sobrevive a recargar y se pierde al cerrar la pestaña.

## README.md (raíz) y backoffice/README.md

Comandos de la pieza `backoffice`:

| Comando | Qué hace |
|---|---|
| `npm start` | Servidor de desarrollo contra el **mock en memoria del contrato** (sin api). Cuentas: `superadmin@kuador.test` y `operador@kuador.test`, con cualquier contraseña |
| `npm run start:api` | Servidor de desarrollo contra una api real (por ejemplo, `infra/local`: `http://admin.localhost:8000` → `http://api.localhost:8000/api/v1`) |
| `npm run build` | Compilación de producción (sin mock ni rutas de desarrollo) |
| `npm test -- --watch=false` | Pruebas unitarias y axe |

## DESIGN.md

El archivo del repositorio sigue en la propuesta v1 (verde #0F6B3E, Inter), mientras que el tablero tiene «Identidad visual» v3 y `design-system/tokens` ya usa los valores de la v2/v3. Sincronizar `DESIGN.md` con la v3 del tablero. Esta tarea implementó los componentes de la v3 que necesitaba: aviso, diálogo, casilla, paginación, tabla (`src/styles/table.css`) y navegación lateral, solo con tokens existentes.
