# ADR-020 · Soporte en el host del complejo con sesión de motivo

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Ajusta:** ADR-011 (quién abre la sesión, en qué estados y con qué cabecera).
- **Contrato:** `contracts/platform-backoffice.openapi.yaml` (etiqueta «Support access») y `contracts/bookings.openapi.yaml`.

## Contexto

Kuador puede leer y editar datos operativos de un complejo para dar soporte, siempre con motivo y auditoría de 1 año (ADR-003). Lo pueden hacer el `SUPER_ADMIN` y el `OPERATOR` con `TENANT_DATA_ACCESS`. En un complejo suspendido o cancelado solo entra el `SUPER_ADMIN`, que puede consultar, crear, bloquear y cancelar (respuestas 1 y 2 del equipo).

## Decisión

1. **Sesión de soporte** abierta en el host global con `POST /api/v1/platform/complexes/{complexId}/support-sessions` y un motivo de 1 a 500 caracteres (no solo espacios, sin datos personales). Queda ligada a la persona y al tenant, dura `support.session-minutes` (30) y se cierra con `DELETE /api/v1/platform/support-sessions/{sessionId}`. Es la sesión de acceso de ADR-011, renombrada y ampliada.
2. **Quién y cuándo:**

   | Estado del tenant | `SUPER_ADMIN` | `OPERATOR` con `TENANT_DATA_ACCESS` |
   |---|---|---|
   | `ACTIVE` | Lectura y edición | Lectura y edición |
   | `SUSPENDED`, `ARCHIVED` | Lectura y edición (lo que cree queda inactivo, ADR-019) | No |
   | `PROVISIONING`, `PROVISIONING_FAILED`, `PURGING`, `PURGED` | No | No |

   Fuera de lo permitido: `403 TENANT_NOT_ACCESSIBLE` al abrir.
3. **Host del complejo.** El backoffice llama a las operaciones de personal existentes en `{subdominio}.playtodoo.com/api/v1` con su token de plataforma y la cabecera **`X-Support-Session`** (reemplaza a `X-Platform-Access-Session`). El tenant se resuelve por subdominio (ADR-002) y en cada solicitud se comprueba que la sesión esté vigente, sea de la persona, su `tenant_id` coincida con el resuelto y el rol siga permitido para el estado actual; si no, `403 SUPPORT_SESSION_INVALID`. Soporte reutiliza los casos de uso del personal: no hay endpoints de soporte duplicados.
4. **Auditoría síncrona antes de ejecutar** (ADR-011): apertura y cada solicitud, permitida o denegada, con `operationId`, método, plantilla de ruta, resultado y código de denegación. Si no se puede registrar, se deniega. Consulta solo del `SUPER_ADMIN` en `GET /complexes/{complexId}/audit-entries`.
5. **Purga.** Al pasar a `PURGING` se invalidan las sesiones abiertas en la misma transacción (ADR-015).
6. **Canchas y horarios para soporte (verificado en PLATODO-81).** No existe ningún endpoint para listar las canchas de un complejo ni sus horarios válidos: los módulos Complejos y canchas y Disponibilidad no existen y `CourtSlotRules` es `UnconfiguredCourtSlotRules`, que informa toda cancha como inexistente. El soporte puede consultar y cancelar reservas y bloqueos (`GET /staff/bookings` trae `courtId`); crear reservas o bloqueos responde `404 COURT_NOT_FOUND` hasta que exista el módulo de canchas, que es configuración interna del complejo y queda fuera de PLATODO-27.
7. **Reemplazo.** `access-sessions`, `access-audit` y la cabecera `X-Platform-Access-Session` de `tenancy.openapi.yaml` quedan `deprecated` y se reemplazan al implementar PLATODO-88. Las tablas `access_session` y `access_audit_event` se amplían por migración (sin perder la auditoría existente).

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Endpoints de soporte bajo `/platform` con `tenantId` en la ruta | Segundo camino de resolución de tenant y casos de uso de Reservas duplicados |
| Solo lectura en suspendidos | El equipo eligió edición (respuesta 2, opción B) |
| Mantener dos mecanismos de sesión (acceso y soporte) | Dos guards con reglas distintas para lo mismo |

## Consecuencias

- Pruebas de concurrencia: soporte contra jugador en un activo, dos sesiones de soporte en un suspendido y soporte contra purga (PLATODO-88).
- Prueba de aislamiento: una sesión del complejo A usada en el host del complejo B se rechaza y se audita.
- **Riesgo:** mientras no exista el módulo de canchas, el soporte no puede crear reservas ni bloqueos (CA11 parcial). Lo debe decidir una persona al planificar ese módulo.
