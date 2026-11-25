# ADR-010 · Persistencia multi-tenant por esquema con falla cerrada

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Épica:** PLATODO-26 · Aislamiento de datos por complejo (decisiones D1 a D4 del diseño de la solución).
- **Relacionados:** ADR-001 (Schema per Tenant), ADR-002 (subdominio), ADR-009 (Flyway).

## Contexto

Con Schema per Tenant (ADR-001) cada complejo tiene su esquema de PostgreSQL. La `api` atiende a todos los complejos con una sola aplicación y un solo pool de conexiones. Hay que garantizar que:

- una solicitud solo lea y escriba en el esquema del tenant resuelto por su host (ADR-002);
- ninguna consulta combine filas de dos esquemas;
- un error de programación (código sin tenant, un hilo asíncrono, una conexión reutilizada) no termine usando un esquema por defecto o el de otra solicitud.

Además existen datos que no son de ningún complejo (tenants, cuentas de jugador, usuarios de Kuador, auditoría) en el esquema `global`.

## Decisión

1. **Dos unidades de persistencia (D1).**
   - **Global:** sin multi-tenancy, sobre el esquema `global`.
   - **Tenant:** Hibernate con multi-tenancy por `SCHEMA`. Los adaptadores de módulos de tenant (por ejemplo, Reservas con `JdbcClient`) usan la misma conexión de tenant.
2. **Ámbito de tenant con `ScopedValue` de Java 25 y falla cerrada (D3).**
   - `TenantScope.run(tenantId, op)` y `TenantScope.call(tenantId, op)` abren el ámbito; solo los abre `TenantResolutionFilter` (solicitudes) o un job o listener que recibe el `tenantId` explícito.
   - `CurrentTenantIdentifierResolver` lee el ámbito. **Sin ámbito activo lanza una excepción**: nunca devuelve un tenant por defecto.
   - El ámbito no se hereda entre hilos: los eventos y jobs llevan el `tenantId` y vuelven a abrir el ámbito.
3. **`search_path` por conexión desde un pool único (D2).**
   - `MultiTenantConnectionProvider` valida el identificador contra `^t_[0-9a-f]{32}$`, fija el esquema del tenant (con el esquema compartido `extensions`, ver PLATODO-29) y, **al devolver la conexión al pool, la deja en `no_tenant`**, un esquema vacío.
   - Una conexión que se reutiliza sin ámbito no ve tablas de ningún complejo: la consulta falla.
4. **Nombre de esquema inmutable `t_<uuid sin guiones>` (D4).** No deriva del subdominio, así cambiar el subdominio (ADR-012) no renombra esquemas, y el nombre nunca viene de la entrada del usuario.
5. **SQL nativo sin esquema calificado** en los módulos de tenant. Lo hace cumplir una regla ArchUnit, además de la revisión de PR.
6. **Pruebas obligatorias:** arnés Testcontainers con dos tenants (A y B); prueba con un pool de una sola conexión que demuestra el reinicio a `no_tenant`; meta-prueba que exige una prueba de aislamiento (`@CoversTenantEntity`) por cada entidad de tenant.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Una sola unidad de persistencia con tablas `global.` calificadas | Obliga a abrir sesiones de tenant con un valor «centinela» para leer lo global y facilita mezclar esquemas por error |
| Un `DataSource` (pool) por tenant | No escala con muchos complejos: conexiones ociosas por cada uno |
| Un rol de base de datos por tenant | Mayor defensa en profundidad, pero costoso de operar en el MVP. Queda como deuda (ver Consecuencias) |
| `ThreadLocal` para el tenant actual | Si no se limpia, filtra el tenant a la siguiente solicitud del mismo hilo; con hilos virtuales es aún más frágil |
| Esquema con el nombre del subdominio | Cambiar el subdominio obligaría a renombrar el esquema y a coordinar ese cambio con todo lo que lo referencia |

## Consecuencias

- Todo acceso a datos de tenant fuera de una solicitud (jobs, listeners asíncronos) debe abrir el ámbito explícitamente; si se olvida, falla en vez de filtrar datos.
- Un solo rol de base de datos tiene permisos sobre todos los esquemas: un SQL que califique otro esquema podría leerlo. Lo mitigan la regla ArchUnit y la revisión. Deuda: evaluar un rol por tenant o Row Level Security cuando haya volumen.
- El esquema `no_tenant` lo crea la migración global (ADR-009).
