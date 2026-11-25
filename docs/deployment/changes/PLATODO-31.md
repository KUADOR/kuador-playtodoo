# Cambios de despliegue · PLATODO-31

## Qué cambia

- **Servidor HTTP en la `api`:** se agrega `spring-boot-starter-webmvc`. Al levantar la `api` (`./mvnw spring-boot:run` o el jar) ahora abre un servidor HTTP (Tomcat) en el puerto por defecto de Spring Boot, `8080` (`server.port` para cambiarlo). [POR DEFINIR: puerto, proxy y dominio por entorno].
- **Nueva propiedad:** `playtodoo.bookings.player-api.enabled` (por defecto `false`). Con `false` no se registra ningún endpoint de reservas y la `api` no necesita base de datos para arrancar.

## Qué hacer al desplegar

| Entorno | Acción |
|---|---|
| Local | Nada obligatorio. Para probar los endpoints, usar las pruebas `*IT` (`./mvnw -B verify`); activarlos a mano no sirve todavía: sin Identidad responden 401. |
| CI | Nada: el workflow `api` ya corre `./mvnw -B verify` con Docker. |
| Entornos reales | Dejar `playtodoo.bookings.player-api.enabled` sin definir o en `false`. Activarla solo cuando estén integradas las tarjetas de Tenancy por solicitud, Identidad (JWT del jugador) y Canchas (`CourtSlotRules`), y con un `DataSource` por tenant configurado. |

Sin secretos ni variables nuevas obligatorias.
