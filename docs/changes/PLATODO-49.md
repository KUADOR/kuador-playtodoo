# Cambios a documentos compartidos · PLATODO-49

## docs/architecture.md

En «Datos», completar la herramienta de migraciones:

- **Migraciones globales:** `api/src/main/resources/db/migration/global` (la primera, `V1__create_no_tenant_schema_and_tenant.sql`: esquema `no_tenant` y tabla `global.tenant`). Las aplica Spring Boot al arrancar (`spring.flyway.schemas=global`).
- **Migraciones de tenant al arrancar:** `GlobalThenTenantMigrations` (estrategia de Flyway) aplica primero las globales y luego `TenantMigrations.migrateAll()`: cada tenant `ACTIVE`, `SUSPENDED` o `ARCHIVED`, de a uno, dentro de su `TenantScope`. El primer error detiene el arranque.

Agregar una sección «Persistencia multi-tenant (ADR-010)»:

```
com.kuador.platodo.api.tenancy/
  application/port/in/           TenantScope (ScopedValue; run, call, current, require),
                                 NoTenantInScopeException
  infrastructure/persistence/    PersistenceConfiguration (pool único y dos unidades de persistencia),
                                 ScopedTenantDataSource (JDBC de módulos de tenant),
                                 SchemaPerTenantConnectionProvider, ScopedTenantIdentifierResolver (Hibernate),
                                 TenantSchemaName (^t_[0-9a-f]{32}$), TenantSearchPath (fija y reinicia el search_path),
                                 TenantMigrations, JdbcMigratableTenants, GlobalThenTenantMigrations
```

- **Unidad global:** JPA sobre `global` (`hibernate.default_schema=global`). Entidades en `com.kuador.platodo.api.*.infrastructure.persistence.global`. Transacciones: bean `globalTransactionManager` (calificador `global`).
- **Unidad de tenant:** Hibernate multi-tenancy por SCHEMA. Entidades en `com.kuador.platodo.api.*.infrastructure.persistence.tenant`. Transacciones: bean `transactionManager` (primario), que también cubre el JDBC sobre `ScopedTenantDataSource`.
- El único `DataSource` inyectable por tipo es `ScopedTenantDataSource`: sin `TenantScope` falla cerrado. El pool (`connectionPool`) solo lo usan Tenancy, la unidad global y Flyway.
- SQL nativo: fuera de `tenancy.infrastructure.persistence` no califica esquemas ni cambia el `search_path` (regla ArchUnit `PersistenceArchitectureTest`).
- Pruebas: cada tabla de `db/migration/tenant` necesita una prueba de aislamiento anotada `@CoversTenantEntity("<tabla>")`; `TenantEntityCoverageIT` lo exige en `./mvnw -B verify`. Arnés de dos tenants: `support/TestTenants` (A y B) y `support/FixedTenantScopeFilter` (sustituto del filtro de subdominio hasta PLATODO-50).

## docs/adr/ADR-010-persistencia-multi-tenant.md (PLATODO-48)

- Las carpetas de migraciones son `db/migration/global` y `db/migration/tenant` (las de ADR-009), no `db/global` y `db/tenant`.
- La meta-prueba `@CoversTenantEntity` cuenta **tablas** del esquema de tenant, estén mapeadas con JPA o escritas con JDBC (Reservas usa `JdbcClient`).

## api/README.md

- `./mvnw spring-boot:run` necesita PostgreSQL levantado (`docker compose up -d --wait`); la conexión se configura con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.
- La prueba de arranque ahora es `PlatodoApiApplicationIT` (necesita Docker); `./mvnw -B package` sigue sin necesitar Docker.
