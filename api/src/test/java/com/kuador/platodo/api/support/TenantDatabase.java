package com.kuador.platodo.api.support;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantSchemaMigrator;

/**
 * One PostgreSQL 18 container shared by the integration tests of a JVM, with the same extensions
 * init script as api/compose.yaml. Testcontainers (Ryuk) removes it when the JVM ends.
 */
public final class TenantDatabase {

    /** Keep in sync with the image in api/compose.yaml. */
    private static final String POSTGRES_IMAGE = "postgres:18-alpine";
    private static final Path EXTENSIONS_SCRIPT = Path.of("docker/postgres/initdb/01-extensions.sql");
    private static final String EXTENSIONS_SCHEMA = "extensions";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(EXTENSIONS_SCRIPT),
                    "/docker-entrypoint-initdb.d/" + EXTENSIONS_SCRIPT.getFileName());

    private TenantDatabase() {
    }

    /** Connections whose search_path is {@code <tenantSchema>, extensions}, as Tenancy sets it. */
    public static DataSource tenantDataSource(String tenantSchema) {
        startOnce();
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        dataSource.setCurrentSchema(tenantSchema + "," + EXTENSIONS_SCHEMA);
        return dataSource;
    }

    /** Creates the tenant schema with the tenant migrations applied and returns its data source. */
    public static DataSource migratedTenant(String tenantSchema) {
        DataSource dataSource = tenantDataSource(tenantSchema);
        new TenantSchemaMigrator(dataSource).migrate(tenantSchema);
        return dataSource;
    }

    private static synchronized void startOnce() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }
}
