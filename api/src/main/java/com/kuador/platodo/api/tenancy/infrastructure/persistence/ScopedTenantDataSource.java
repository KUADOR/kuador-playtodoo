package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * Data source for plain JDBC adapters of tenant modules (for example Bookings with {@code JdbcClient}). Each
 * connection uses only the schema of the tenant in {@link TenantScope}; without a scope it fails closed with
 * {@link NoTenantInScopeException}. Closing the connection resets it to {@code no_tenant} before it returns to the
 * pool.
 */
public final class ScopedTenantDataSource extends DelegatingDataSource {

    private static final String CLOSE = "close";
    private static final String IS_CLOSED = "isClosed";
    private static final String EQUALS = "equals";
    private static final String HASH_CODE = "hashCode";
    private static final String TO_STRING = "toString";

    public ScopedTenantDataSource(DataSource pool) {
        super(pool);
    }

    @Override
    public Connection getConnection() throws SQLException {
        TenantSchemaName schema = TenantSchemaName.of(TenantScope.require());
        return resettingOnClose(TenantSearchPath.open(obtainTargetDataSource(), schema));
    }

    /** Per-call credentials would bypass the pool and its reset; tenant connections always come from the pool. */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Tenant connections come from the shared pool only");
    }

    private static Connection resettingOnClose(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                ScopedTenantDataSource.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                new ResetOnClose(target));
    }

    private static final class ResetOnClose implements InvocationHandler {

        private final Connection target;
        private boolean closed;

        private ResetOnClose(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case CLOSE -> {
                    if (!closed) {
                        closed = true;
                        TenantSearchPath.release(target);
                    }
                    return null;
                }
                case IS_CLOSED -> {
                    return closed || target.isClosed();
                }
                case EQUALS -> {
                    return proxy == args[0];
                }
                case HASH_CODE -> {
                    return System.identityHashCode(proxy);
                }
                case TO_STRING -> {
                    return "Tenant connection [" + target + "]";
                }
                default -> {
                    if (closed) {
                        throw new SQLException("The tenant connection is closed");
                    }
                    try {
                        return method.invoke(target, args);
                    } catch (InvocationTargetException error) {
                        throw error.getTargetException();
                    }
                }
            }
        }
    }
}
