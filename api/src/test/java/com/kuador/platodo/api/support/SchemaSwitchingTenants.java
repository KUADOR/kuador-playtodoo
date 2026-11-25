package com.kuador.platodo.api.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.kuador.platodo.api.tenancy.application.port.in.ActiveTenantRunner;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Test stand-in for Tenancy: a fixed list of active tenants (tenant id = schema name) and one shared
 * data source whose connections use the schema of the tenant being visited on the current thread.
 */
public final class SchemaSwitchingTenants implements ActiveTenantRunner {

    private static final String EXTENSIONS_SCHEMA = "extensions";

    private final ThreadLocal<String> currentSchema = new ThreadLocal<>();
    private final List<TenantId> activeTenants;
    private final DataSource dataSource;

    public SchemaSwitchingTenants(List<String> activeTenantSchemas) {
        this.activeTenants = activeTenantSchemas.stream().map(TenantId::new).toList();
        this.dataSource = new CurrentTenantDataSource(TenantDatabase.tenantDataSource(EXTENSIONS_SCHEMA));
    }

    /** Shared data source; only usable inside {@link #forEachActiveTenant}. */
    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void forEachActiveTenant(Consumer<TenantId> work) {
        for (TenantId tenant : activeTenants) {
            currentSchema.set(tenant.value());
            try {
                work.accept(tenant);
            } finally {
                currentSchema.remove();
            }
        }
    }

    private final class CurrentTenantDataSource extends DelegatingDataSource {

        private CurrentTenantDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            String schema = currentSchema.get();
            if (schema == null) {
                throw new IllegalStateException("No tenant is active on this thread");
            }
            Connection connection = super.getConnection();
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO \"" + schema + "\", " + EXTENSIONS_SCHEMA);
            } catch (SQLException error) {
                connection.close();
                throw error;
            }
            return connection;
        }
    }
}
