# Producto: Playtodoo Kuador

## Usuarios

- **Núcleo:**
  - **Jugadores** (app móvil): personas que quieren practicar un deporte, buscan una cancha cercana con horario libre y la reservan. El pago se hace directamente en el complejo.
  - **Complejos deportivos** (propietarios, administradores y personal autorizado, en la app web): administran canchas, horarios, reservas, novedades, torneos, usuarios y su suscripción. Son los clientes que pagan a Kuador.
- **Secundario:** el equipo interno de Kuador (backoffice global), que da de alta y gestiona complejos, planes, suscripciones y pagos de suscripción, y consulta métricas globales.

## Propósito

Reservar una cancha debería ser tan simple como pedir un servicio desde el móvil. Playtodoo resuelve dos problemas:

- **Jugadores:** encontrar canchas cercanas, ver horarios disponibles y reservar sin llamadas ni mensajes.
- **Complejos:** administrar instalaciones, horarios y reservas con herramientas digitales, sin conflictos de horario ni reservas duplicadas.

Kuador monetiza vendiendo suscripciones a los complejos (modelo SaaS).

Éxito, según los pilares de la idea:

- Reservas duplicadas o con conflicto de horario: cero.
- Ningún complejo accede a datos de otro complejo.
- Complejos con suscripción activa.
- Reservas creadas por complejo.
- Metas numéricas y plazos: [POR DEFINIR: valores objetivo y fechas para cada indicador].

## Posicionamiento

Diferenciadores que surgen de la idea:

- Una sola plataforma conecta a jugadores con muchos complejos y entrega a cada complejo su propio espacio de gestión (canchas, horarios, reservas, novedades y torneos).
- Cada complejo gestiona exclusivamente sus propios datos (aislamiento por tenant).
- La reserva es sencilla y no exige pago en la app durante el MVP.

Alternativas y competidores con los que se compara: [POR DEFINIR: alternativas actuales (llamadas, WhatsApp, otras apps) y mercados objetivo].

## Contexto operativo

- **Idioma:** español.
- **Pago de reservas (MVP):** el jugador no paga en la app; paga en el complejo.
- **Cobro de suscripciones (MVP):** transferencia o factura fuera de la plataforma. La plataforma solo registra y da seguimiento a la suscripción y a sus pagos.
- **Identificación del complejo:** por subdominio (complejo.playtodoo.com).
- **Acceso de Kuador a datos de un complejo:** lectura y edición, con motivo registrado y auditoría.
- **Cancelaciones:** se rigen por la política de cada complejo: [POR DEFINIR: qué parámetros puede configurar un complejo (plazo, penalización, etc.)].
- **País, moneda, zona horaria y regulación** (facturación, protección de datos): [POR DEFINIR: país de lanzamiento y obligaciones legales].
- **Integraciones locales** (mapas, notificaciones, facturación): [POR DEFINIR: cuáles se usarán en el MVP].
- **Entrega:** las tres aplicaciones (móvil de jugadores, web de complejos y backoffice global) se construyen en paralelo sobre un backend único.
