# Recorrido de extremo a extremo · suscripciones y backoffice (corrida 1795790823)

Entorno local por subdominios (`*.localhost:41000`), api real, sin mock. 2026-11-27T14:47:03Z.

| # | CA | Paso | Solicitud | Esperado | Obtenido | Resultado |
|---|---|---|---|---|---|---|
| 1 | — | Login del SUPER_ADMIN | `POST api/auth/platform/login` | 200 | 200 | OK |
| 2 | — | Login del OPERATOR | `POST api/auth/platform/login` | 200 | 200 | OK |
| 3 | CA15 | El OPERATOR empieza sin funciones | `GET api/platform/me` | 200 OPERATOR 0 | 200 OPERATOR 0 | OK |
| 4 | CA4 | Crear un campo de plan sin cambio de código | `POST api/platform/plan-fields` | 201 CUSTOM | 201 CUSTOM | OK |
| 5 | CA4 | Editar el campo | `PUT api/platform/plan-fields/{id}` | 200 Soporte 24/7 1795790823 | 200 Soporte 24/7 1795790823 | OK |
| 6 | CA4 | Retirar el campo | `POST api/platform/plan-fields/{id}/retirement` | 200 retirado | 200 retirado | OK |
| 7 | CA4 | Un campo básico no se retira | `POST api/platform/plan-fields/{básico}/retirement` | 422 BASIC_FIELD_READ_ONLY | 422 BASIC_FIELD_READ_ONLY | OK |
| 8 | CA3 | Crear plan (borrador) | `POST api/platform/plans` | 201 DRAFT | 201 DRAFT | OK |
| 9 | CA3 | Un plan no publicado no se asigna | `POST api/platform/complexes` | 422 PLAN_NOT_PUBLISHED | 422 PLAN_NOT_PUBLISHED | OK |
| 10 | CA3 | Publicar el plan | `POST api/platform/plans/{id}/publication` | 200 PUBLISHED | 200 PUBLISHED | OK |
| 11 | CA3 | Publicar un segundo plan | `POST api/platform/plans/{id}/publication` | 200 PUBLISHED | 200 PUBLISHED | OK |
| 12 | CA1 | Alta del complejo A | `POST api/platform/complexes` | 201 ACTIVE ACTIVE | 201 ACTIVE ACTIVE | OK |
| 13 | CA1 | Alta del complejo B | `POST api/platform/complexes` | 201 ACTIVE ACTIVE | 201 ACTIVE ACTIVE | OK |
| 14 | CA1 | Cada complejo tiene su esquema | `SQL pg_namespace` | 1 1 | 1 1 | OK |
| 15 | CA1 | A responde en su subdominio | `GET subsa1795790823/tenant` | 200 Complejo Suscripción A | 200 Complejo Suscripción A | OK |
| 16 | CA2 | Subdominio repetido | `POST api/platform/complexes` | 409 SUBDOMAIN_TAKEN | 409 SUBDOMAIN_TAKEN | OK |
| 17 | CA2 | El rechazo no crea nada | `SQL count(global.tenant)` | 5 | 5 | OK |
| 18 | CA2 | La disponibilidad lo marca en uso | `GET api/subdomains/{sub}/availability` | 200 false TAKEN | 200 false TAKEN | OK |
| 19 | CA5 | Cambio de plan de A | `PUT api/platform/complexes/{A}/plan` | 200 Plan E2E Pro 1795790823 | 200 Plan E2E Pro 1795790823 | OK |
| 20 | CA6 | Cambio manual a «por vencer» | `POST api/platform/complexes/{A}/status-changes` | 200 EXPIRING ACTIVE | 200 EXPIRING ACTIVE | OK |
| 21 | CA6 | Queda el responsable del cambio | `GET api/platform/complexes/{A}` | E2E Super 1795790823 | E2E Super 1795790823 | OK |
| 22 | CA7 | Registrar pago de A | `POST api/platform/complexes/{A}/payments` | 201 100.00 TRANSFER | 201 100.00 TRANSFER | OK |
| 23 | CA7 | El pago está en el historial de pagos | `GET api/platform/complexes/{A}/payments` | 200 1 | 200 1 | OK |
| 24 | CA5 | Historial con el cambio de plan (anterior y nuevo) | `GET api/platform/complexes/{A}/history` | Plan E2E Base 1795790823 → Plan E2E Pro 1795790823 | Plan E2E Base 1795790823 → Plan E2E Pro 1795790823 | OK |
| 25 | CA6 | Historial con el cambio de estado y su responsable | `GET api/platform/complexes/{A}/history` | ACTIVE → EXPIRING E2E Super 1795790823 | ACTIVE → EXPIRING E2E Super 1795790823 | OK |
| 26 | CA19 | Listado con plan, estado y último pago de A | `GET api/platform/complexes` | Plan E2E Pro 1795790823 EXPIRING 100.00 | Plan E2E Pro 1795790823 EXPIRING 100.00 | OK |
| 27 | CA15 | El OPERATOR lee el listado | `GET api/platform/complexes` | 200 | 200 | OK |
| 28 | CA15 | OPERATOR sin la función no registra pagos | `POST api/platform/complexes/{A}/payments` | 403 | 403 | OK |
| 29 | CA15 | OPERATOR sin la función no da de alta | `POST api/platform/complexes` | 403 | 403 | OK |
| 30 | CA15 | OPERATOR sin la función no accede a datos | `POST api/platform/complexes/{A}/support-sessions` | 403 | 403 | OK |
| 31 | CA15 | El cambio de plan es solo del SUPER_ADMIN | `PUT api/platform/complexes/{A}/plan` | 403 | 403 | OK |
| 32 | CA16 | El OPERATOR no configura funciones | `PUT api/platform/operator-capabilities` | 403 | 403 | OK |
| 33 | CA16 | SUPER_ADMIN habilita «registrar pagos» | `PUT api/platform/operator-capabilities` | 200 true | 200 true | OK |
| 34 | CA16 | Aplica: el OPERATOR registra el pago | `POST api/platform/complexes/{A}/payments` | 201 | 201 | OK |
| 35 | CA15 | Lo no habilitado se sigue denegando | `POST api/platform/complexes` | 403 | 403 | OK |
| 36 | CA16 | Retirada la función, se deniega en la siguiente solicitud | `POST api/platform/complexes/{A}/payments` | 403 CAPABILITY_REVOKED | 403 CAPABILITY_REVOKED | OK |
| 37 | CA16 | Los cambios quedan registrados con responsable | `GET api/platform/operator-capabilities/changes` | 2 E2E Super 1795790823 | 2 E2E Super 1795790823 | OK |
| 38 | CA18 | Login del personal de A en A | `POST subsa1795790823/auth/staff/login` | 200 | 200 | OK |
| 39 | CA18 | El token del personal no sirve en el backoffice | `GET api/platform/complexes` | 401 | 401 | OK |
| 40 | CA18 | Ni para identificarse como Kuador | `GET api/platform/me` | 401 | 401 | OK |
| 41 | CA18 | Credenciales del personal de A en B | `POST subsb1795790823/auth/staff/login` | 401 INVALID_CREDENTIALS | 401 INVALID_CREDENTIALS | OK |
| 42 | CA18 | Token del personal de A en el host de B | `GET subsb1795790823/staff/bookings` | 401 | 401 | OK |
| 43 | CA12 | Sin sesión de soporte no se muestran datos | `GET subsa1795790823/staff/bookings` | 403 SUPPORT_SESSION_INVALID sin datos | 403 SUPPORT_SESSION_INVALID sin datos | OK |
| 44 | CA12 | Sin motivo se deniega | `POST api/platform/complexes/{A}/support-sessions` | 422 REASON_REQUIRED | 422 REASON_REQUIRED | OK |
| 45 | CA13 | Con motivo se abre la sesión | `POST api/platform/complexes/{A}/support-sessions` | 201 subsa1795790823.localhost ACTIVE | 201 subsa1795790823.localhost ACTIVE | OK |
| 46 | CA13 | Lectura de soporte en A | `GET subsa1795790823/staff/bookings + X-Support-Session` | 200 4 false | 200 4 false | OK |
| 47 | CA13 | Edición de soporte en A (cancelar reserva) | `POST subsa1795790823/staff/bookings/{id}/cancel + X-Support-Session` | 200 CANCELLED | 200 CANCELLED | OK |
| 48 | CA14 | La sesión de A no abre B | `GET subsb1795790823/staff/bookings + sesión de A` | 403 SUPPORT_SESSION_INVALID | 403 SUPPORT_SESSION_INVALID | OK |
| 49 | CA14 | Ni permite editar en B | `POST subsb1795790823/staff/bookings/{id de A}/cancel + sesión de A` | 403 SUPPORT_SESSION_INVALID | 403 SUPPORT_SESSION_INVALID | OK |
| 50 | CA14 | La reserva de A sigue intacta y B no tiene reservas | `SQL t_ed78f2e9df5c496483a171c9439e2237 / t_a751ef35937d4d9aa7760b64d614fa2a` | CONFIRMED 0 | CONFIRMED 0 | OK |
| 51 | CA13 | Auditoría de A: persona, motivo, operación y resultado | `GET api/platform/complexes/{A}/audit-entries` | E2E Super 1795790823 · El complejo pidió revisar una reserva duplicada · ALLOWED | E2E Super 1795790823 · El complejo pidió revisar una reserva duplicada · ALLOWED | OK |
| 52 | CA13 | La auditoría registra también lo denegado | `GET api/platform/complexes/{A}/audit-entries` | sí | sí | OK |
| 53 | CA14 | La auditoría de B no muestra la sesión de A | `GET api/platform/complexes/{B}/audit-entries` | 0 | 0 | OK |
| 54 | CA15 | La auditoría es solo del SUPER_ADMIN | `GET api/platform/complexes/{A}/audit-entries` | 403 | 403 | OK |
| 55 | CA13 | Cerrar la sesión | `DELETE api/platform/support-sessions/{id}` | 204 | 204 | OK |
| 56 | CA12 | Una sesión cerrada ya no muestra datos | `GET subsa1795790823/staff/bookings + sesión cerrada` | 403 SUPPORT_SESSION_INVALID | 403 SUPPORT_SESSION_INVALID | OK |
| 57 | CA17 | 10 cancelaciones simultáneas de una reserva (personal y 2 sesiones de soporte) | `POST subsa1795790823/staff/bookings/{id}/cancel ×10` | 10 respondidas, 0 errores | 10 respondidas, 0 errores | OK |
| 58 | CA17 | La reserva queda cancelada una sola vez | `SQL t_ed78f2e9df5c496483a171c9439e2237.booking` | CANCELLED 1 | CANCELLED 1 | OK |
| 59 | CA8 | Suspender A | `POST api/platform/complexes/{A}/status-changes` | 200 SUSPENDED SUSPENDED | 200 SUSPENDED SUSPENDED | OK |
| 60 | CA9 | Corre el plazo de conservación (45 días) | `GET api/platform/complexes/{A}` | 45 sí | 45 sí | OK |
| 61 | CA8 | El personal no entra a A suspendido | `POST subsa1795790823/auth/staff/login` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 62 | CA8 | Ni con un token emitido antes | `GET subsa1795790823/staff/bookings` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 63 | CA8 | El jugador no reserva en A suspendido | `POST subsa1795790823/bookings` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 64 | CA8 | A no se muestra en el catálogo de cercanos | `SQL global.complex_catalog.visible` | false | false | OK |
| 65 | CA8 | B sigue visible | `SQL global.complex_catalog.visible` | true | true | OK |
| 66 | CA11 | El OPERATOR habilitado no entra a un suspendido | `POST api/platform/complexes/{A}/support-sessions` | 403 TENANT_NOT_ACCESSIBLE | 403 TENANT_NOT_ACCESSIBLE | OK |
| 67 | CA11 | El SUPER_ADMIN entra con motivo | `POST api/platform/complexes/{A}/support-sessions` | 201 SUSPENDED | 201 SUSPENDED | OK |
| 68 | CA9 | Las reservas futuras se conservan, inactivas | `GET subsa1795790823/staff/bookings + X-Support-Session` | 200 2 true | 200 2 true | OK |
| 69 | CA11 | El SUPER_ADMIN edita en un suspendido (cancelar) | `POST subsa1795790823/staff/bookings/{id}/cancel + X-Support-Session` | 200 CANCELLED true | 200 CANCELLED true | OK |
| 70 | CA9 | Reactivar A antes del plazo | `POST api/platform/complexes/{A}/status-changes` | 200 ACTIVE | 200 ACTIVE | OK |
| 71 | CA9 | El personal recupera el acceso | `POST subsa1795790823/auth/staff/login` | 200 | 200 | OK |
| 72 | CA9 | La reserva conservada vuelve intacta y activa | `GET subsa1795790823/staff/bookings` | 200 CONFIRMED false | 200 CONFIRMED false | OK |
| 73 | CA9 | A vuelve al catálogo | `SQL global.complex_catalog.visible` | true | true | OK |
| 74 | CA10 | Cancelar A | `POST api/platform/complexes/{A}/status-changes` | 200 CANCELLED ARCHIVED | 200 CANCELLED ARCHIVED | OK |
| 75 | CA10 | Una cancelada no se reactiva | `POST api/platform/complexes/{A}/status-changes` | 422 TRANSITION_NOT_ALLOWED | 422 TRANSITION_NOT_ALLOWED | OK |
| 76 | CA8 | Cancelada bloquea igual que suspendida | `GET subsa1795790823/tenant` | 403 TENANT_UNAVAILABLE | 403 TENANT_UNAVAILABLE | OK |
| 77 | CA10 | Vencido el plazo, la tarea programada purga A | `GET api/platform/complexes/{A}` | 200 PURGED sí | 200 PURGED sí | OK |
| 78 | CA10 | El esquema de A ya no existe | `SQL pg_namespace` | 0 | 0 | OK |
| 79 | CA10 | A sale del catálogo | `SQL global.complex_catalog` | sin fila | sin fila | OK |
| 80 | CA10 | Se redacta el correo del propietario | `SQL global.tenant.owner_email` | redactado | redactado | OK |
| 81 | CA10 | El subdominio de A ya no da acceso | `GET subsa1795790823/tenant` | 404 TENANT_NOT_FOUND | 404 TENANT_NOT_FOUND | OK |
| 82 | CA14 | La purga deja intacto el esquema de B | `SQL pg_namespace` | 1 | 1 | OK |
| 83 | CA14 | B sigue activo tras la purga de A | `GET subsb1795790823/tenant` | 200 Complejo Suscripción B | 200 Complejo Suscripción B | OK |
| 84 | CA10 | Se conservan los pagos de Kuador | `GET api/platform/complexes/{A}/payments` | 200 3 | 200 3 | OK |
| 85 | CA5 | Se conserva el historial de plan y estado | `GET api/platform/complexes/{A}/history` | 200 sí | 200 sí | OK |
| 86 | CA13 | Se conserva la auditoría sin ningún motivo legible | `GET api/platform/complexes/{A}/audit-entries` | 200 sí 0 | 200 sí 0 | OK |

**Resultado: los 86 pasos dieron lo esperado.**
