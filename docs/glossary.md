# Glosario de Playtodoo Kuador

| Término | Significado | No confundir con |
|---|---|---|
| Playtodoo | Plataforma que conecta jugadores con complejos deportivos para buscar y reservar canchas | Kuador |
| Kuador | Empresa dueña de la plataforma; vende suscripciones a los complejos | Complejo |
| Complejo deportivo | Negocio que ofrece canchas y es cliente de Kuador mediante una suscripción | Cancha |
| Tenant | Cada complejo como unidad aislada de la plataforma, con su propio esquema de datos | Usuario |
| Esquema (schema) | Espacio propio de un complejo dentro de PostgreSQL | Esquema global, que guarda datos de la plataforma |
| Subdominio | Dirección que identifica al complejo (complejo.playtodoo.com) | Dominio principal |
| Cancha | Instalación deportiva reservable dentro de un complejo | Complejo |
| Jugador | Usuario final que busca canchas y reserva desde la app móvil | Personal del complejo |
| Personal del complejo | Propietarios, administradores y usuarios autorizados del complejo | Usuario global |
| Usuario global | Miembro del equipo interno de Kuador | Personal del complejo |
| Horario | Franja en que una cancha puede usarse, según la configuración del complejo | Reserva |
| Disponibilidad | Horarios de una cancha que aún no están reservados | Horario |
| Reserva | Apartado de una cancha para una fecha y un horario; se paga en el complejo | Pago de suscripción |
| Calendario de ocupación | Vista del complejo con las canchas y horarios ocupados | Disponibilidad |
| Política de cancelación | Reglas del complejo para que el jugador cancele una reserva | Cancelación de suscripción |
| Novedad | Aviso o noticia que el complejo publica para los jugadores | Torneo |
| Torneo | Competencia publicada y administrada por un complejo | Novedad |
| Plan comercial | Oferta de suscripción de Kuador con precio y características | Suscripción |
| Suscripción | Contrato vigente de un complejo con un plan; se cobra por transferencia o factura fuera de la plataforma | Plan comercial |
| Pago de suscripción | Pago del complejo a Kuador, registrado para seguimiento | Pago de reserva |
| Motivo de acceso | Justificación obligatoria cuando Kuador lee o edita datos de un complejo | Permiso |
| Auditoría | Registro de los accesos de Kuador a datos operativos de un complejo | Historial de reservas |
| `backoffice` (pieza) | Aplicación Angular del equipo Kuador (backoffice global) | La web del complejo |
| `app` (pieza) | Aplicación web React del complejo (en la idea original, «Backoffice») | `backoffice` |
| `movil` (pieza) | Aplicación Flutter de los jugadores | `app` |
