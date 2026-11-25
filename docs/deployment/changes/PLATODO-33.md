# Cambios de despliegue · PLATODO-33

## Qué cambia

- **Nueva propiedad:** `playtodoo.bookings.staff-api.enabled` (por defecto `false`). Con `false` no se registra ningún endpoint del personal (`/api/v1/staff/...`). Es independiente de `playtodoo.bookings.player-api.enabled`.

## Qué hacer al desplegar

| Entorno | Acción |
|---|---|
| Local | Nada obligatorio. Para probar los endpoints, usar las pruebas `*IT` (`./mvnw -B verify`); activarlos a mano no sirve todavía: sin Identidad responden 401. |
| CI | Nada: el workflow `api` ya corre `./mvnw -B verify` con Docker. |
| Entornos reales | Dejar `playtodoo.bookings.staff-api.enabled` sin definir o en `false`. Activarla solo cuando estén integradas Tenancy por solicitud, Identidad (rol de personal del tenant, `CurrentStaff`) y Canchas (`CourtSlotRules`), con un `DataSource` por tenant configurado. |

Sin secretos, servicios ni migraciones nuevas.
