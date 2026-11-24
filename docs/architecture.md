# Arquitectura de Playtodoo Kuador

> Los ejemplos de este documento son ilustrativos. Usan nombres ficticios (por ejemplo, «Canchas El Sol») para aclarar cómo funciona cada idea. No son datos del negocio ni decisiones tomadas. Lo que sigue en [POR DEFINIR] sigue pendiente.

## Stack

_Lo escribe el tablero a partir de las piezas del proyecto (Configuración → Piezas y repositorio): no lo edites a mano._

| Pieza | Tipo | Stack | Dónde | Compilar | Probar | Levantar |
|---|---|---|---|---|---|---|
| api | Backend | Java 25 · Spring Boot 4 | `api/` | `./mvnw -B package` | `./mvnw -B verify` | `./mvnw spring-boot:run` |
| backoffice | Frontend | Angular · TypeScript | `backoffice/` | `npm run build` | `npm test -- --watch=false` | `npm start` |
| app | Frontend | React · Vite · TypeScript | `app/` | `pnpm build` | `pnpm test` | `pnpm dev` |
| movil | Móvil | Flutter | `movil/` | — | — | — |

## Piezas y quién las usa

Nota de nombres: en la idea original «Backoffice» designaba la web de los complejos y «Backoffice global» la de Kuador. En las piezas del tablero, `backoffice` es el de Kuador. Este documento sigue las piezas.

| Pieza | Usuarios | Función |
|---|---|---|
| `api` | Todas las apps | Backend centralizado con la lógica de negocio y el aislamiento por tenant |
| `backoffice` | Equipo Kuador | Backoffice global (Angular): complejos, tenants, planes, suscripciones, pagos de suscripción, usuarios globales, parámetros y métricas |
| `app` | Propietarios, administradores y personal del complejo | Web del complejo (React): configuración, canchas, horarios, reservas, calendario, novedades, torneos, usuarios y permisos, plan y suscripción |
| `movil` | Jugadores | Registro, búsqueda de complejos cercanos, horarios, reservas, historial, novedades y torneos |

Ejemplo de uso de cada pieza:

- Una persona del equipo de Kuador entra a `backoffice` y suspende al complejo ficticio «Canchas El Sol» por falta de pago de la suscripción.
- La administradora de «Canchas El Sol» entra a `app` en `canchaselsol.playtodoo.com` y bloquea la cancha 2 el sábado por mantenimiento.
- Una jugadora abre `movil`, busca complejos cercanos y reserva la cancha 1 para el viernes a las 20:00.

La persona que dio el brief llamó «la app» al cliente del jugador. Se asume que ese cliente es `movil` (Flutter) y que `app` (React) es la web del complejo. [POR DEFINIR: confirmar esta asignación].

## Arquitectura del backend

La `api` es un **monolito modular**: una sola aplicación desplegable, dividida en módulos con responsabilidades y fronteras claras (ver Módulos). Cada módulo sigue una **arquitectura hexagonal** (puertos y adaptadores). La comunicación entre módulos se hace con **Spring Events**.

- **Monolito modular:** un único despliegue. Cada módulo es dueño de su lógica y de sus datos, y no accede directamente a las clases internas ni a las tablas de otro módulo.
- **Arquitectura hexagonal por módulo:**
  - **Dominio:** reglas de negocio, sin dependencias de framework ni de infraestructura.
  - **Aplicación:** casos de uso y puertos de entrada y de salida.
  - **Adaptadores de entrada:** API REST, consumida por las tres apps.
  - **Adaptadores de salida:** persistencia en PostgreSQL e integraciones externas, siempre detrás de una interfaz (puerto).
- **Spring Events:** un módulo publica eventos de dominio y otros módulos reaccionan sin acoplarse entre sí. [POR DEFINIR: catálogo de eventos y cuáles se procesan de forma síncrona o asíncrona].
- **Multi-tenant:** la resolución del tenant y el esquema activo se manejan de forma transversal en el módulo Tenancy, no dentro de cada caso de uso.
- **Verificación de fronteras entre módulos:** [POR DEFINIR: herramienta para hacerlas cumplir (por ejemplo, Spring Modulith o pruebas de arquitectura)].

### Ejemplo: capas hexagonales del módulo Reservas (ilustrativo)

Los nombres son ilustrativos y no fijan la estructura final de paquetes.

```
reservas/
  dominio/
    Reserva                  (entidad: cancha, fecha, horario, estado)
    PoliticaCancelacion      (regla: ¿se puede cancelar a estas horas?)
  aplicacion/
    entrada/
      CrearReserva           (puerto de entrada, caso de uso)
      CancelarReserva        (puerto de entrada, caso de uso)
    salida/
      ReservaRepositorio     (puerto de salida, interfaz)
  infraestructura/
    entrada/
      ReservaController      (adaptador REST)
    salida/
      ReservaRepositorioPostgres   (adaptador de persistencia)
```

El dominio no conoce Spring ni PostgreSQL. El controlador llama al puerto de entrada y el caso de uso usa el puerto de salida. Cambiar la persistencia no toca las reglas de negocio.

### Ejemplo: comunicación entre módulos con Spring Events (ilustrativo)

Una jugadora cancela una reserva:

1. El módulo **Reservas** cancela la reserva según la política del complejo y publica el evento `ReservaCancelada` (cancha, fecha, horario).
2. El módulo **Disponibilidad** escucha el evento y vuelve a marcar ese horario como libre.
3. Reservas no llama directamente a Disponibilidad ni conoce sus clases internas.

Este evento es solo un ejemplo del mecanismo. El catálogo real de eventos sigue [POR DEFINIR].

## Arquitectura del frontend

Son tres clientes independientes que consumen la misma `api`. Ninguno accede a la base de datos. El tenant se identifica por subdominio en la web del complejo (ver ADR-002).

| Pieza | Tecnología | Arquitectura | Notas |
|---|---|---|---|
| `movil` | Flutter | [POR DEFINIR: organización de capas y gestión de estado] | Jugadores. Sin pagos dentro de la app |
| `app` | React | [POR DEFINIR: organización de carpetas, gestión de estado y acceso a datos] | Web del complejo. Solo ve datos de su propio tenant |
| `backoffice` | Angular | [POR DEFINIR: organización de carpetas, gestión de estado y acceso a datos] | Equipo Kuador. Acceso a datos de complejos con permiso y auditoría |

Criterios comunes propuestos para los tres, a confirmar:

- Organización por funcionalidad, alineada con los módulos del backend.
- Acceso a la `api` aislado en una capa propia, separada de la interfaz.
- Autenticación y roles gestionados de forma centralizada en cada cliente.
- [POR DEFINIR: sistema de diseño compartido entre `app` (React) y `backoffice` (Angular). Al ser frameworks distintos no se pueden compartir componentes; solo se podrían compartir criterios visuales (colores, tipografías, espaciados). ¿Se hace así o cada web tiene su propia librería?].

Ejemplo de los criterios comunes aplicados a la funcionalidad «reservas» en `app` (ilustrativo, no fija la organización final):

```
reservas/
  api/        (llamadas a la api de Reservas, sin lógica de pantalla)
  pantallas/  (lista de reservas, calendario de ocupación)
  componentes/
```

La carpeta se llama igual que el módulo `Reservas` del backend. Si cambia la forma de llamar a la `api`, solo se toca la subcarpeta `api/`.

## Datos

- **Base de datos:** PostgreSQL con estrategia Schema per Tenant: cada complejo tiene su propio esquema.
- **Esquema global** (propuesta): tenants, planes, suscripciones, pagos de suscripción, usuarios globales, parámetros de plataforma, auditoría y catálogo de complejos (ver abajo).
- **Herramienta de migraciones:** [POR DEFINIR]. Debe cubrir tanto el esquema global como la creación y evolución del esquema de cada tenant.

Ejemplo de cómo se ve la base con dos complejos ficticios (los nombres de esquema son ilustrativos):

| Esquema | Contiene |
|---|---|
| `global` | tenants, planes, suscripciones, pagos de suscripción, usuarios globales, parámetros, auditoría, catálogo de complejos |
| esquema de «Canchas El Sol» | canchas, horarios, reservas, novedades, torneos y usuarios de ese complejo |
| esquema de «Complejo La Esquina» | canchas, horarios, reservas, novedades, torneos y usuarios de ese complejo |

Una solicitud a `canchaselsol.playtodoo.com` solo puede leer y escribir en el esquema de «Canchas El Sol». Las reservas de «Complejo La Esquina» no son visibles desde ahí.

### Búsqueda de complejos cercanos entre tenants (propuesta, a confirmar)

Problema: con Schema per Tenant, la ubicación de cada complejo vive en su propio esquema. Consultar «los complejos cerca del jugador» obligaría a recorrer todos los esquemas, lo que es lento y rompe el aislamiento.

Propuesta: un **catálogo global de complejos** en el esquema global, con solo datos públicos, y separar la consulta en dos pasos.

1. **Búsqueda de cercanos (esquema global).** La consulta de ubicación se resuelve únicamente contra el catálogo global. Un jugador envía su ubicación y recibe los complejos cercanos ordenados por distancia. No se toca ningún esquema de tenant.
   - Contenido del catálogo (propuesta): identificador del tenant, subdominio, nombre, dirección, latitud y longitud, contacto, estado del tenant (solo se listan los activos). [POR DEFINIR: deportes o tipos de cancha, para poder filtrar].
   - Cálculo de distancia: [POR DEFINIR: extensión PostGIS con índice espacial, o cálculo de distancia con latitud y longitud e índice por rango].
2. **Detalle, canchas y horarios (esquema del tenant).** Al elegir un complejo, la `api` resuelve su tenant, activa su esquema y consulta allí canchas, disponibilidad y reservas. Estas consultas nunca cruzan tenants.

Ejemplo de filas del catálogo global (datos ficticios):

| Tenant | Subdominio | Nombre | Dirección | Lat / Lon | Estado |
|---|---|---|---|---|---|
| t-001 | canchaselsol | Canchas El Sol | Calle Ejemplo 123 | 0.0000 / 0.0000 | activo |
| t-002 | laesquina | Complejo La Esquina | Av. Ficticia 456 | 0.0000 / 0.0000 | suspendido |

Ejemplo de recorrido completo:

1. La jugadora abre `movil` y envía su ubicación.
2. La `api` consulta solo el catálogo global y devuelve «Canchas El Sol», ordenado por distancia. «Complejo La Esquina» no aparece porque está suspendido.
3. La jugadora elige «Canchas El Sol». La `api` resuelve el tenant `t-001`, activa su esquema y devuelve canchas y horarios libres.

Sincronización del catálogo:

- La fuente de verdad de la información del complejo sigue siendo su esquema. El módulo Complejos y canchas publica un evento (Spring Events) cuando cambian datos públicos o ubicación, y el catálogo global se actualiza a partir de él.
- La activación o suspensión del tenant (módulo Tenancy) actualiza el estado en el catálogo, de modo que un complejo suspendido deja de aparecer.
- [POR DEFINIR: cómo se obtienen la latitud y longitud (el complejo marca el punto en un mapa o se calculan a partir de la dirección)].
- [POR DEFINIR: si el MVP permite filtrar los cercanos por disponibilidad en una fecha y hora. Eso requeriría consultar cada tenant o mantener una copia agregada de disponibilidad; la propuesta actual lo deja fuera y muestra la disponibilidad solo al entrar al complejo].

Ejemplo de sincronización: la administradora de «Canchas El Sol» corrige el teléfono de contacto en `app`. El módulo Complejos y canchas guarda el cambio en el esquema del complejo y publica el evento de cambio de datos públicos. El catálogo global actualiza la fila `t-001`.

## Módulos

Propuesta inicial del backend `api`, a confirmar al diseñar. Cada módulo expone su API pública mediante sus puertos de entrada y los eventos que publica.

| Módulo | Responsabilidad | API pública |
|---|---|---|
| Tenancy | Resolver el complejo por subdominio, crear y activar o suspender esquemas | Resolución de tenant por solicitud; ciclo de vida del tenant |
| Identidad y acceso | Registro e inicio de sesión de jugadores, usuarios del complejo y usuarios globales; roles y permisos | [POR DEFINIR] |
| Complejos y canchas | Información comercial, ubicación, contacto y canchas; publicar los datos públicos al catálogo global de complejos y atender la búsqueda de cercanos | [POR DEFINIR] |
| Disponibilidad | Horarios y disponibilidad por cancha | [POR DEFINIR] |
| Reservas | Crear, consultar y cancelar reservas según la política del complejo, sin duplicados ni conflictos | [POR DEFINIR] |
| Novedades | Publicación y consulta de novedades | [POR DEFINIR] |
| Torneos | Publicación y administración de torneos | [POR DEFINIR] |
| Planes y suscripciones | Planes, precios, características y seguimiento de suscripciones y pagos externos | [POR DEFINIR] |
| Auditoría | Registrar accesos de Kuador a datos de complejos, con motivo | [POR DEFINIR] |
| Métricas | Indicadores globales para Kuador | [POR DEFINIR] |

Ejemplo de qué módulos participan al crear una reserva (ilustrativo):

1. `movil` envía la solicitud a la `api` con el subdominio del complejo.
2. **Tenancy** resuelve el tenant y activa su esquema.
3. **Identidad y acceso** comprueba que la jugadora está autenticada.
4. **Reservas** valida que el horario esté libre y crea la reserva. Si ya está ocupado, responde con un conflicto (ver ADR-006).
5. **Disponibilidad** actualiza el horario como ocupado al recibir el evento correspondiente.

## Integraciones

No hay integraciones externas definidas para el MVP. El cobro de suscripciones es manual (transferencia o factura) y el jugador no paga en la app.

[POR DEFINIR: mapas y geolocalización, notificaciones y correo]. Cada integración se aísla detrás de una interfaz propia del módulo que la usa (puerto de salida en la arquitectura hexagonal).

Ejemplo: si se decide enviar correos de confirmación de reserva, el módulo Reservas depende de un puerto de salida `NotificadorDeReservas`. Un adaptador implementa ese puerto con el proveedor elegido. Cambiar de proveedor solo cambia el adaptador. El proveedor concreto sigue [POR DEFINIR].

## Decisiones (ADR)

- **ADR-001 · Schema per Tenant en PostgreSQL** ({date}): cada complejo es un tenant con su propio esquema, para garantizar que gestione solo sus datos.
- **ADR-002 · Tenant identificado por subdominio** ({date}): el complejo se resuelve por subdominio (complejo.playtodoo.com), tanto en la web del complejo como cuando el jugador consulta sus canchas. Ejemplo: `canchaselsol.playtodoo.com` resuelve al tenant «Canchas El Sol».
- **ADR-003 · Acceso de Kuador con motivo y auditoría** ({date}): el equipo de Kuador puede leer y editar datos operativos de un complejo solo con permiso y con un motivo registrado en la auditoría. Ejemplo: una persona de soporte de Kuador necesita ver las reservas de «Canchas El Sol» por un reclamo. Antes de acceder registra el motivo («reclamo por reserva duplicada») y queda en la auditoría con quién, cuándo y a qué accedió.
- **ADR-004 · Sin pagos en la plataforma en el MVP** ({date}): el jugador paga la reserva en el complejo y el complejo paga su suscripción a Kuador por transferencia o factura. La plataforma solo registra el seguimiento. Ejemplo: la jugadora reserva en `movil` y paga en la recepción del complejo; el complejo transfiere su suscripción mensual a Kuador y el equipo de Kuador registra ese pago en `backoffice`.
- **ADR-005 · Tres aplicaciones en paralelo sobre un backend único** ({date}): móvil, web del complejo y backoffice global se construyen a la vez contra la misma `api`.
- **ADR-006 · Prevención de reservas duplicadas** ({date}): el sistema nunca debe permitir dos reservas en la misma cancha y horario. [POR DEFINIR: mecanismo técnico (restricción en base de datos, bloqueo, etc.)]. Ejemplo del caso a evitar: dos jugadores piden a la vez la cancha 1 del viernes a las 20:00. Solo una reserva se confirma; la otra recibe un aviso de que el horario ya no está disponible.
- **ADR-007 · Monolito modular con arquitectura hexagonal y Spring Events** ({date}): el backend `api` es un monolito modular. Cada módulo sigue arquitectura hexagonal y los módulos se comunican entre sí mediante Spring Events, sin acceder a las partes internas de otros módulos.
- **ADR-008 · Catálogo global de complejos para la búsqueda de cercanos** ({date}): propuesta a confirmar. La búsqueda por ubicación se resuelve en el esquema global con datos públicos de los complejos, sincronizados mediante eventos desde cada tenant. Los datos operativos (canchas, horarios, reservas) se consultan solo en el esquema del tenant elegido, sin consultas entre esquemas.
