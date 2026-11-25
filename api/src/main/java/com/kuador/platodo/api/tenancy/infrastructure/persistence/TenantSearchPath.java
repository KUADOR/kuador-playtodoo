package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

/**
 * Points a pooled connection at one tenant schema and back at the empty {@code no_tenant} schema (ADR-010, D2).
 * Every connection leaves tenant use at {@code no_tenant}, so a reused connection without a tenant sees no tenant
 * table. If the reset fails the physical connection is aborted: the pool discards it instead of reusing it.
 */
final class TenantSearchPath {

    /** Empty schema created by the global migrations; the search_path of every idle pooled connection. */
    static final String NO_TENANT_SCHEMA = "no_tenant";
    /** Shared schema with database extensions such as btree_gist (api/README.md). */
    static final String EXTENSIONS_SCHEMA = "extensions";
    /** Leaves a connection without any tenant schema. */
    static final String RESET_SQL = "SET search_path TO " + NO_TENANT_SCHEMA;

    private static final Executor ABORT_IN_CALLER = Runnable::run;

    private TenantSearchPath() {
    }

    /** Borrows a connection from {@code pool} whose search_path is {@code <schema>, extensions}. */
    static Connection open(DataSource pool, TenantSchemaName schema) throws SQLException {
        Connection connection = pool.getConnection();
        try {
            execute(connection, "SET search_path TO " + schema.value() + ", " + EXTENSIONS_SCHEMA);
            return connection;
        } catch (SQLException | RuntimeException error) {
            release(connection);
            throw error;
        }
    }

    /**
     * Resets {@code connection} to {@code no_tenant} and returns it to the pool. An open transaction is rolled back
     * first: a SET inside a transaction that is rolled back later would be undone.
     */
    static void release(Connection connection) throws SQLException {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
            execute(connection, RESET_SQL);
        } catch (SQLException | RuntimeException error) {
            connection.abort(ABORT_IN_CALLER);
            throw error;
        } finally {
            connection.close();
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
