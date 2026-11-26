# ADR-019 · Reservas inactivas derivadas del estado del tenant

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-26
- **Épica:** PLATODO-27 · Suscripciones y backoffice de Kuador.
- **Relacionado:** ADR-006 (prevención de reservas duplicadas).
- **Contrato:** `contracts/bookings.openapi.yaml` (`StaffBooking.inactive`).

## Contexto

El equipo decidió que al suspender un complejo sus reservas futuras se conservan, inactivas, hasta reactivarlo o hasta que venza el plazo. Además, el `SUPER_ADMIN` puede crear, bloquear y cancelar en un complejo suspendido (respuesta 2, opción B).

## Decisión

1. **No se escribe en las reservas** al suspender, cancelar ni reactivar. «Inactiva» se deriva del estado del tenant: toda entrada que devuelven las operaciones de personal de `bookings.openapi.yaml` lleva `inactive: true` si el tenant está `SUSPENDED` o `ARCHIVED`, y `false` si está `ACTIVE`. Sin columna nueva ni cambio del esquema del complejo.
2. **Lo que crea el soporte** en un suspendido queda igualmente inactivo y sigue las mismas reglas de ADR-006 (sin solapes, con la misma restricción de exclusión).
3. **Reactivar** devuelve las reservas tal como estaban; en un cancelado nunca se activan.
4. El pendiente de 5 minutos sigue venciendo igual: en un suspendido ningún jugador puede confirmar.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Marcar cada reserva como inactiva | Escritura masiva, riesgo de conflictos con ADR-006 y la reactivación debe restaurar el estado exacto |
| Cancelar las reservas al suspender | El equipo pidió conservarlas |

## Consecuencias

- El cálculo vive en la presentación de Reservas, que consulta el estado del tenant resuelto (ya disponible en el filtro).
- Prueba: suspender y reactivar no modifica ninguna fila de `booking`.
