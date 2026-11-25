# ADR-011 · Sesiones de acceso de Kuador y auditoría síncrona

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Épica:** PLATODO-26 · Aislamiento de datos por complejo (decisiones D6, D7 y D12 del diseño de la solución).
- **Detalla:** ADR-003 (acceso de Kuador con motivo y auditoría).
- **Contrato:** `contracts/tenancy.openapi.yaml` (etiqueta «Platform access»).

## Contexto

ADR-003 permite al equipo de Kuador leer y editar datos operativos de un complejo solo con permiso y un motivo auditado. El Equipo Kuador decidió que **solo el dueño de Kuador** (`PLATFORM_OWNER`) puede hacerlo; otros roles se analizarán más adelante. Hay que definir cómo se pide ese acceso, cómo se limita a un complejo y cómo se garantiza que nunca haya un acceso sin registro.

## Decisión

1. **Sesión de acceso con motivo.** El dueño de Kuador abre una sesión en el host global (`POST /api/v1/platform/tenants/{tenantId}/access-sessions`) con un motivo obligatorio (texto no vacío). La sesión:
   - queda ligada a un solo tenant y al usuario que la abrió;
   - dura 30 minutos por defecto (parámetro de seguridad configurable, no de negocio);
   - se puede cerrar antes (`DELETE /api/v1/platform/access-sessions/{id}`).
2. **Acceso por el host del complejo (D7).** El `backoffice` llama a `{subdomain}.playtodoo.com/api/...` con su token `PLATFORM` y la cabecera `X-Platform-Access-Session`. Así la resolución del tenant y el aislamiento son los mismos que para el personal (ADR-002, ADR-010).
3. **`PlatformAccessGuard`** (Tenancy) valida el rol, que la sesión esté vigente y que su `tenant_id` sea el tenant resuelto. Si no, responde `403 ACCESS_SESSION_INVALID`.
4. **Auditoría síncrona antes del acceso (D6).** El guard llama a `Auditoría.RecordAccess` en una transacción propia **antes** de ejecutar la operación. Si el registro falla, el acceso se deniega. Los intentos denegados también se registran.
5. **Plantilla de ruta, no ruta concreta (D12).** La auditoría guarda método, plantilla de ruta (por ejemplo `/api/v1/staff/bookings/{bookingId}`), resultado, quién, cuándo y el motivo de la sesión. Nunca la ruta con ids ni el query string.
6. **CORS** en los hosts de tenant: solo el origen del `backoffice` (`admin.playtodoo.com`), además del propio host.
7. **Datos personales en el motivo.** La ayuda del campo en el `backoffice` pide no escribirlos. Si el complejo se purga, el motivo se anonimiza (ADR-013).

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Auditoría por evento asíncrono | Si el evento se pierde o falla, habría un acceso sin registro: incumple ADR-003 |
| Proxy por ruta en el host global (`api.playtodoo.com/platform/tenants/{id}/...`) | Duplica la resolución de tenant fuera del filtro y contradice ADR-002 |
| Guardar la ruta concreta y el query string | Puede contener ids y datos personales y dificulta anonimizar |
| Permiso permanente por rol, sin sesión | No obliga a declarar un motivo por cada acceso ni lo limita en el tiempo |

## Consecuencias

- Cada solicitud de Kuador a un complejo hace una escritura síncrona en `global.access_audit_event`: un poco más de latencia, aceptable por el bajo volumen.
- Un fallo de la base global bloquea el acceso de Kuador a los complejos: es intencional (falla cerrada).
- Las tablas `access_session` y `access_audit_event` son del módulo Auditoría; Tenancy solo llama a su puerto.
