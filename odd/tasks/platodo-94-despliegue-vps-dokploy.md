# Seguimiento: Despliegue VPS - Dokploy

Épica PLATODO-94. Historia, criterios y decisiones: documento «Seguimiento» del tablero; diseño:
`docs/solution/platodo-94-despliegue-vps-dokploy.md`. Manual: `docs/despliegue-dokploy-develop.md`.

## Clasificación

- **Comprensión:** media-alta. Dominio (`develop.play.kuador.com`), datos (solo ficticios) y ubicación del manual
  (`docs/`) decididos por el equipo.
- **Riesgo:** medio. No toca producción ni código de negocio; trata secretos (solo nombres) y expone un ambiente de
  pruebas.
- **Revisión:** fácil, es documentación. Se valida siguiendo los pasos en un VPS real.

## Alcance

Un manual en `docs/` para desplegar `api`, `app`, `backoffice` y PostgreSQL en Dokploy como ambiente de pruebas
alimentado por `develop`, con los ajustes y secretos requeridos (solo nombres). Fuera: producción, cambios de código
en las piezas, `movil`, pagos, monitoreo, respaldos, valores de secretos y datos reales.

## Criterios de aceptación

1. Existe el manual en `docs/`, en español, con nombres técnicos, archivos y comandos en inglés.
2. Siguiendo los pasos de Dokploy en un VPS quedan desplegados api, app, backoffice y PostgreSQL.
3. Un push a `develop` despliega las piezas actualizadas sin pasos manuales.
4. El inventario de variables y secretos lista solo nombres y propósito.
5. Bajo `develop.play.kuador.com`, app y backoffice se conectan a la api y el tenant se resuelve por subdominio.
6. El manual declara que solo se usan datos ficticios.
7. La lista de verificación confirma las cuatro piezas; hay diagnóstico de fallos comunes.
8. La validación en el VPS queda registrada en este documento.

## Tareas

- [x] Inventario de variables y secretos de api, app y backoffice (PLATODO-95).
- [x] Esquema de subdominios bajo `develop.play.kuador.com` (PLATODO-95).
- [x] Sección de Dokploy: proyecto, servicios, base de datos, redes y dominios (PLATODO-95).
- [x] Sección de GitHub: conexión del repositorio, rama `develop` y secretos (PLATODO-95).
- [x] Conexión de app y backoffice con la api y subdominio del tenant (PLATODO-95).
- [x] Uso exclusivo de datos ficticios (PLATODO-95).
- [x] Lista de verificación post-despliegue y diagnóstico de fallos comunes (PLATODO-95).
- [x] Ensayo local del manual, con evidencia (PLATODO-96).
- [ ] Validar el manual en el VPS y registrar la evidencia (PLATODO-96). **Bloqueada:** no hay VPS, panel de Dokploy
  ni registros DNS disponibles.

## Estado

**2026-11-27 · PLATODO-96.** El manual **no se validó en un VPS**. Desde el runner no hay acceso a un VPS ni a un
panel de Dokploy, y `*.develop.play.kuador.com` no resuelve (`dig +short` vacío para la raíz, `admin.` y un
subdominio cualquiera): sigue abierta la condición del equipo sobre el DNS de `kuador.com`.

Se hizo lo que no depende de eso: un ensayo local con las mismas piezas que usa Dokploy (builds con Railpack,
PostgreSQL 18, Traefik v3.6 con las rutas del manual). El ensayo encontró tres fallos del manual, ya corregidos
(ver «Correcciones»), y deja comprobado el comportamiento de las piezas. Los criterios 2, 3, 5 y 8 **siguen
abiertos** hasta repetirlo en el VPS.

| Criterio | Estado |
|---|---|
| 2 · Las cuatro piezas desplegadas en un VPS | Pendiente del VPS. En local: las cuatro levantan con los builds y variables del manual |
| 3 · Un push a `develop` despliega solo | Pendiente del VPS. No se puede ensayar sin Dokploy y su GitHub App |
| 5 · Conexión con la api y tenant por subdominio | Comprobado en local con los hosts reales. Pendiente repetirlo con DNS y certificados reales |
| 8 · Evidencia de la validación en el VPS | Pendiente. Este documento registra solo el ensayo local |

## Evidencias

### Ensayo local · 2026-11-27 · commit `ceef6ab`

**Entorno.** macOS arm64, Docker 29.2.1. Railpack 0.44.0 (`railpack prepare` + `docker buildx build` con su
frontend, como hace Dokploy). `postgres:18-alpine` (18.7), `traefik:v3.6` (3.6.25). Una red con subred fija en el
papel de `dokploy-network`; solo Traefik publica puertos. Los hosts reales (`*.develop.play.kuador.com`) se
resolvieron a `127.0.0.1` en `curl` y en Chrome; HTTPS con el certificado autofirmado de Traefik. Secretos generados
en el momento y borrados al terminar. Solo datos ficticios (`example.test`). Archivos del ensayo:
`docs/evidence/platodo-96/ensayo/`.

**Builds (secciones 5.4 y 5.5).**

| Pieza | Variables | Resultado |
|---|---|---|
| `api` | `RAILPACK_JDK_VERSION=25`, `RAILPACK_BUILD_CMD` | Correcto. JDK 25.0.5; arranca en el puerto 8080 sin definir `PORT` |
| `app` | `RAILPACK_NODE_VERSION=22` | Correcto. Node 22.23.5, pnpm 12.23.0, `pnpm run build`; Caddy en el puerto 80 |
| `backoffice` | `RAILPACK_NODE_VERSION=22`, `RAILPACK_SPA_OUTPUT_DIR` | Correcto. Caddy en el puerto 80 |
| `api` sin `RAILPACK_BUILD_CMD` | — | Falla en `failsafe`: `Could not find a valid Docker environment` |
| `backoffice` sin `RAILPACK_SPA_OUTPUT_DIR` | — | Falla: `"/app/browser": not found` |
| `app` con Nixpacks 1.41.0 | `NIXPACKS_NODE_VERSION=22` | Falla: `Cannot find module …/corepack/pnpm/12.23.0/bin/pnpm.cjs` |
| `backoffice` con Nixpacks 1.41.0 | `NIXPACKS_NODE_VERSION=22` | Falla: `The Angular CLI requires a minimum Node.js version of v22.22.3` |

**Arranque.** Con las variables obligatorias de la sección 4 y `01-extensions.sql` aplicado antes, la api aplica las
11 migraciones de `global` y termina en `Started PlatodoApiApplication`. Carga de datos de la sección 8 seguida tal
cual: persona de Kuador, parámetros de plan, plan publicado y dos complejos (`canchas-el-sol`, `la-esquina`) por la
api, ambos `ACTIVE`.

**Lista de verificación (sección 10).**

| # | Comprobación | Resultado |
|---|---|---|
| 1 | Esquema de `btree_gist` | Correcto: `extensions` |
| 2 | Historial de Flyway | Correcto: 12 filas, ninguna fallida |
| 3 | PostgreSQL sin puerto público | Parcial: el contenedor no publica puertos. El cortafuegos del VPS queda pendiente |
| 4 | `api.…/subdomains/www/availability` | Correcto: `200`, `"reason":"RESERVED"` |
| 5 | Login de jugador inexistente | Correcto: `401` `INVALID_CREDENTIALS` |
| 6 | `…/api/v1/tenant` en cada complejo | Correcto: `200` con `Canchas El Sol` y `La Esquina`, cada uno en su host |
| 7 | `api.…/api/v1/tenant` | Correcto: `404` `RESOURCE_NOT_FOUND` |
| 8 | Preflight CORS | Correcto: `200` con `Access-Control-Allow-Origin` para `admin.…`; `403` con otro origen |
| 9 | `app` en Chrome, con recarga en una ruta interna | Correcto: muestra «Canchas El Sol»; `GET /api/v1/tenant` `200` en su mismo origen. Captura: `docs/evidence/platodo-96/app-canchas-el-sol.png` |
| 10 | `backoffice` en Chrome: login y lista de complejos | Correcto: llama a `https://api.…`, lista los dos complejos, sin errores de red, CORS ni consola. Captura: `docs/evidence/platodo-96/backoffice-complejos.png` |
| 11 | Personal de un complejo en el host del otro | Correcto: `401` `INVALID_CREDENTIALS` en ambos sentidos; `200` en el propio |
| 12 | TLS | Parcial: HTTP redirige a HTTPS (`301`) y todo el recorrido fue por HTTPS, con certificado autofirmado. Let's Encrypt queda pendiente del VPS |
| 13 | Auto-despliegue | **Pendiente del VPS** |
| 14 | Secretos en el diff | Correcto: solo nombres |

**Aislamiento entre los dos complejos** (con `PLAYTODOO_BOOKINGS_STAFF_API_ENABLED=true` y una reserva ficticia
solo en `canchas-el-sol`):

| Prueba | Resultado |
|---|---|
| El personal de `canchas-el-sol` lista sus reservas | `200`, una reserva |
| El personal de `la-esquina` lista las suyas | `200`, lista vacía |
| El personal de `la-esquina` pide esa reserva por su id | `404` `BOOKING_NOT_FOUND` |
| Token de `canchas-el-sol` en el host de `la-esquina`, y al revés | `401` `UNAUTHENTICATED` |
| Token de Kuador en el host de un complejo, sin sesión de soporte | `403` `SUPPORT_SESSION_INVALID` |
| `X-Forwarded-Host` de otro complejo enviado por el cliente | Ignorado: responde el complejo del host real |
| Host no declarado en el proxy | `404 page not found` de Traefik, como dice la sección 2 |

**Correcciones al manual que salieron del ensayo.**

1. `app` y `backoffice` pasan de Nixpacks a Railpack: con Nixpacks ninguna de las dos compila.
2. `api` necesita `RAILPACK_BUILD_CMD`: sin ella el build ejecuta las pruebas de integración y falla.
3. `backoffice` necesita `RAILPACK_SPA_OUTPUT_DIR=dist/backoffice/browser`.

Nota para el release: `docs/deployment/changes/PLATODO-96.md`.

**Fallos de código en api, app o backoffice:** ninguno. No se abre tarjeta aparte.

### Pendiente de comprobar en el VPS

- Formularios y nombres de campos de Dokploy v0.31.4; versión de Railpack que trae y que pase las variables al build.
- Registros DNS, certificados de Let's Encrypt por host y cortafuegos (filas 3 y 12).
- GitHub App de Dokploy y auto-despliegue filtrado por `Watch Paths` (fila 13, criterio 3).
- Subred real de `dokploy-network` en `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`.
- Anexo A (comodín): sin ensayar.

### Validación en el VPS · pendiente

Decisión del equipo (2026-11-27): una persona sigue el manual en el VPS y deja los resultados en la tarjeta
PLATODO-96; el agente DevOps los registra aquí. El manual a seguir es el de la rama
`feature/PLATODO-96-validar-el-manual-en-el-vps-y-registrar` (PR #44), que ya trae las correcciones del ensayo.

Datos que hay que dejar en la tarjeta, **sin valores de secretos, IP ni capturas con contraseñas**:

- Fecha, commit de `develop` desplegado, versión de Dokploy y valor del campo `Railpack Version`.
- Resultado de cada fila de la sección 10 del manual (1 a 14): correcto o falla, y en las fallas el mensaje.
- Fila 13: qué piezas desplegaron tras fusionar un PR que toca solo `app/`.
- Cada paso del manual que no coincidió con Dokploy (campo con otro nombre, paso que faltó o sobró).
- Los puntos marcados **[POR VALIDAR]** en el manual: cómo resultaron.
