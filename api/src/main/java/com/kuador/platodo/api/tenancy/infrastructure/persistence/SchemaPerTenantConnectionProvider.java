package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

/**
 * Hibernate connections of the tenant persistence unit (ADR-010). The tenant identifier is the schema name, checked
 * against {@code ^t_[0-9a-f]{32}$} before it reaches SQL; every released connection goes back to {@code no_tenant}.
 */
public final class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource pool;

    public SchemaPerTenantConnectionProvider(DataSource pool) {
        this.pool = pool;
    }

    /** Used by Hibernate for metadata only; the connection stays at {@code no_tenant}. */
    @Override
    public Connection getAnyConnection() throws SQLException {
        return pool.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        TenantSearchPath.release(connection);
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        return TenantSearchPath.open(pool, new TenantSchemaName(tenantIdentifier));
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        TenantSearchPath.release(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /** This provider sets the schema itself, so Hibernate must not change it. */
    @Override
    public boolean handlesConnectionSchema() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> type) {
        return type.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (!isUnwrappableAs(type)) {
            throw new IllegalArgumentException("Cannot unwrap to " + type.getName());
        }
        return type.cast(this);
    }
}
