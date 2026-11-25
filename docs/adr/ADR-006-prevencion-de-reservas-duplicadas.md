# ADR-006 · Prevención de reservas duplicadas

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Épica:** PLATODO-25 · Reservas sin conflictos ni duplicados. Diseño en `docs/solution/platodo-25-reservas-sin-conflictos-ni-duplicados.md`.
- **Contrato:** `contracts/bookings.openapi.yaml`.

## Contexto

Una cancha nunca puede tener dos reservas o bloqueos activos cuyos horarios se solapen, ni siquiera con solicitudes simultáneas, desde `movil` o `app`, y siempre dentro del esquema del complejo resuelto por subdominio (ADR-001, ADR-002). Reglas de producto ya decididas:

- Una reserva del jugador nace **pendiente** y retiene la franja **5 minutos**; si se confirma pasa a **confirmada**; si vence, pasa a **vencida** y libera la franja.
- Pendiente vigente, confirmada y bloqueo ocupan la franja; cancelada y vencida la liberan.
- La duración es variable por cancha (la configura el dueño). Cualquier solape parcial es conflicto.
- El personal crea reservas manuales, cancela y bloquea con la misma regla. Un bloqueo nunca anula una reserva: se rechaza hasta que el personal la cancele.
- Un jugador puede tener varias reservas en el mismo horario (para compañeros), con una advertencia previa.
- Una inserción directa duplicada en la base también debe fallar (criterio 14).

## Decisión

1. **Una tabla `booking` por esquema de tenant** guarda reservas y bloqueos (`kind = RESERVATION | BLOCK`) con el horario como rango semiabierto `tstzrange [inicio, fin)`.
2. **Restricción de exclusión de PostgreSQL** (extensión `btree_gist`):
   ```sql
   CONSTRAINT booking_no_overlap EXCLUDE USING gist (court_id WITH =, period WITH &&)
     WHERE (status IN ('PENDING','CONFIRMED'))
   ```
   La base rechaza con SQLSTATE `23P01` cualquier fila activa que se solape con otra de la misma cancha, venga de la `api` o de un `INSERT` directo. 20:00–21:00 y 21:00–22:00 no chocan; 20:00–21:00 y 20:30–21:30 sí.
3. **Traducción del error:** el adaptador de persistencia convierte `23P01` en un error de dominio y la API responde `409` con Problem Details y `code` estable (`SLOT_UNAVAILABLE`, o `SLOT_HAS_BOOKING` con `conflictingBookingId` cuando un bloqueo choca con una reserva). El texto en español lo pone el cliente.
4. **Pendiente de 5 minutos:** el servidor fija `expires_at` con un `Clock` inyectable; el plazo es una constante del dominio. El vencimiento se aplica de tres formas complementarias:
   - **Perezoso:** en la misma transacción y antes del `INSERT`, se marcan `EXPIRED` los pendientes vencidos de esa cancha que se solapan con el rango pedido.
   - **Tarea programada:** recorre los tenants activos de a uno (vía Tenancy), marca `EXPIRED` los pendientes vencidos y publica `BookingExpired`. Es idempotente; varias instancias pueden ejecutarla a la vez.
   - **Estado efectivo en lecturas:** un `PENDING` con `expires_at` pasado se informa como `EXPIRED`.
5. **Confirmar** es un `UPDATE` condicional (`status = 'PENDING' AND expires_at > now`). Si no actualiza filas, la API responde `409 BOOKING_EXPIRED` (o `BOOKING_NOT_PENDING` si fue cancelada).
6. **La duración** se toma de la cancha al crear (puerto `CourtSlotRules`) y queda fija en la fila.
7. **Advertencia de varias reservas** en el servidor (`acknowledgeOwnOverlap`), solo dentro del mismo complejo para no cruzar esquemas.
8. **Pruebas** de persistencia y concurrencia contra PostgreSQL real con Testcontainers (N hilos sobre la misma franja: exactamente 1 éxito y N-1 `SLOT_UNAVAILABLE`), más una prueba de aislamiento con dos tenants.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| `SELECT … FOR UPDATE` o `pg_advisory_xact_lock` por cancha | Solo protege el camino de la aplicación; una inserción directa la salta (criterio 14) |
| `UNIQUE (court_id, start)` | No detecta solapes parciales con duraciones variables |
| Aislamiento `SERIALIZABLE` | Reintentos costosos y tampoco protege inserciones directas |
| Tabla de bloqueos aparte con trigger | Una restricción de exclusión cubre una sola tabla; el trigger reabre carreras |
| Vencimiento solo por tarea programada | Deja una ventana en la que un pendiente vencido sigue ocupando |
| `pg_cron` | Infraestructura extra en cada entorno |

## Consecuencias

- La garantía vive en la base: no depende de que todos los caminos de código hagan la comprobación.
- Requiere la extensión `btree_gist` en cada base (local, CI y entornos); la instala la tarea de DevOps (PLATODO-29) en un esquema compartido de extensiones incluido en el `search_path` de cada tenant. No contiene datos de complejos.
- H2 u otra base en memoria no sirven para estas pruebas (no soportan `EXCLUDE` ni `tstzrange`).
- La migración de la tabla se aplica a todos los esquemas de tenant (ADR-009).
- Disponibilidad no guarda copia de la ocupación (decisión D4 en `docs/architecture.md`).
