# Recorrido de extremo a extremo · tenancy (corrida 1795653706)

Entorno local por subdominios (`*.localhost:8000`), api real. 2026-11-26T00:41:46Z.

| # | Paso | Solicitud | Esperado | Obtenido | Resultado |
|---|---|---|---|---|---|
| 1 | Login del dueño de Kuador (host global) | `POST api/auth/platform/login` | 200 | 200 | OK |
| 2 | Alta del complejo A | `POST api/platform/tenants` | 201 ACTIVE | 201 ACTIVE | OK |
| 3 | Alta del complejo B | `POST api/platform/tenants` | 201 ACTIVE | 201 ACTIVE | OK |
| 4 | Complejo activo | `GET e2ea1795653706/tenant` | 200 Complejo E2E A | 200 Complejo E2E A | OK |
| 5 | Login del personal de A en A | `POST e2ea1795653706/auth/staff/login` | 200 | 200 | OK |
| 6 | Credenciales del personal de A en B | `POST e2eb1795653706/auth/staff/login` | 401 INVALID_CREDENTIALS | 401 INVALID_CREDENTIALS | OK |
| 7 | Token del personal de A en el host de B | `PUT e2eb1795653706/tenant/subdomain` | 401 UNAUTHENTICATED | 401 UNAUTHENTICATED | OK |
| 8 | Login del personal de B en B | `POST e2eb1795653706/auth/staff/login` | 200 | 200 | OK |
| 9 | Suspender A | `POST api/platform/tenants/{A}/suspend` | 200 SUSPENDED | 200 SUSPENDED | OK |
| 10 | Complejo suspendido | `GET e2ea1795653706/tenant` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 11 | Login del personal en A suspendido | `POST e2ea1795653706/auth/staff/login` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 12 | B sigue activo mientras A está suspendido | `GET e2eb1795653706/tenant` | 200 Complejo E2E B | 200 Complejo E2E B | OK |
| 13 | Activar A | `POST api/platform/tenants/{A}/activate` | 200 ACTIVE | 200 ACTIVE | OK |
| 14 | Archivar A | `POST api/platform/tenants/{A}/archive` | 200 ARCHIVED 45 | 200 ARCHIVED 45 | OK |
| 15 | Complejo archivado | `GET e2ea1795653706/tenant` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 16 | Reactivar A | `POST api/platform/tenants/{A}/reactivate` | 200 ACTIVE | 200 ACTIVE | OK |
| 17 | Complejo reactivado | `GET e2ea1795653706/tenant` | 200 Complejo E2E A | 200 Complejo E2E A | OK |
| 18 | El personal de A recupera su acceso | `POST e2ea1795653706/auth/staff/login` | 200 | 200 | OK |
| 19 | Subdominio inexistente | `GET e2ex1795653706/tenant` | 404 TENANT_NOT_FOUND | 404 TENANT_NOT_FOUND | OK |
| 20 | Subdominio mal formado | `GET ab/tenant` | 404 TENANT_NOT_FOUND | 404 TENANT_NOT_FOUND | OK |
| 21 | El propietario cambia el subdominio de A | `PUT e2ea1795653706/tenant/subdomain` | 200 e2ea1795653706-nuevo | 200 e2ea1795653706-nuevo | OK |
| 22 | El subdominio nuevo resuelve al mismo complejo | `GET e2ea1795653706-nuevo/tenant` | 200 Complejo E2E A | 200 Complejo E2E A | OK |
| 23 | El subdominio anterior deja de resolver | `GET e2ea1795653706/tenant` | 404 TENANT_NOT_FOUND | 404 TENANT_NOT_FOUND | OK |
| 24 | El token del propietario sigue sirviendo en el host nuevo | `PUT e2ea1795653706-nuevo/tenant/subdomain` | 200 | 200 | OK |
| 25 | Otro complejo no puede tomar el subdominio retirado | `POST api/platform/tenants` | 409 SUBDOMAIN_TAKEN | 409 SUBDOMAIN_TAKEN | OK |

**Resultado: los 25 pasos dieron lo esperado.**
