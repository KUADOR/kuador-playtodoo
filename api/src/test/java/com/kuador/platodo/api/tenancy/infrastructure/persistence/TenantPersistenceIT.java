package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.support.infrastructure.persistence.tenant.ProbeNote;
import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Schema-per-tenant persistence on a pool of ONE connection, wired like production by {@link PersistenceConfiguration}:
 * every use reuses the same physical connection, so a search_path left behind by a tenant would leak into the next
 * use. The application context only provides the database, its migrations and two tenants (the startup migrations
 * need more than one connection). Numbers refer to the criteria of PLATODO-26.
 */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantPersistenceIT {

    private static final String SEARCH_PATH_AND_BACKEND =
            "SELECT current_setting('search_path') || '@' || pg_backend_pid()";
    private static final String NO_TENANT = TenantSearchPath.NO_TENANT_SCHEMA;
    private static final int ONE_CONNECTION = 1;

    @Autowired
    private DataSourceProperties connectionSettings;
    @Autowired
    private Environment environment;
    @Autowired
    private TenantMigrations migrations;

    private HikariDataSource pool;
    private ScopedTenantDataSource tenantData;
    private EntityManagerFactory tenantUnit;
    private TransactionTemplate transactions;

    @BeforeAll
    void wireASingleConnectionPool() {
        TestTenants.provision(migrations, TestTenants.A);
        TestTenants.provision(migrations, TestTenants.B);

        PersistenceConfiguration configuration = new PersistenceConfiguration();
        pool = configuration.connectionPool(connectionSettings, environment);
        pool.setMaximumPoolSize(ONE_CONNECTION);
        tenantData = configuration.tenantDataSource(pool);
        LocalContainerEntityManagerFactoryBean factory = configuration.tenantEntityManagerFactory(pool);
        factory.afterPropertiesSet();
        tenantUnit = factory.getObject();
        transactions = new TransactionTemplate(configuration.transactionManager(tenantUnit, tenantData));
    }

    @AfterAll
    void closeThePool() {
        tenantUnit.close();
        pool.close();
    }

    @Test
    void aConnectionUsedByTenantAComesBackAtNoTenantForWorkWithoutTenant_criterion18() throws SQLException {
        String insideA = TenantScope.call(TestTenants.A, () -> searchPathAndBackend(tenantData));
        String withoutTenant = searchPathAndBackend(pool);

        assertThat(searchPath(insideA)).isEqualTo(schemaOf(TestTenants.A) + ", extensions");
        assertThat(searchPath(withoutTenant)).isEqualTo(NO_TENANT);
        assertThat(backend(withoutTenant)).as("same physical connection").isEqualTo(backend(insideA));
    }

    @Test
    void aTenantConnectionClosedInsideAnOpenTransactionStillComesBackAtNoTenant() throws SQLException {
        TenantScope.call(TestTenants.A, () -> {
            try (Connection connection = tenantData.getConnection()) {
                connection.setAutoCommit(false);
                execute(connection, "SELECT 1");
            }
            return null;
        });

        assertThat(searchPath(searchPathAndBackend(pool))).isEqualTo(NO_TENANT);
    }

    @Test
    void hibernateSessionsUseTheScopedSchemaAndReleaseTheConnectionAtNoTenant() throws SQLException {
        String insideB = TenantScope.call(TestTenants.B, () -> transactions.execute(status ->
                (String) currentEntityManager().createNativeQuery("SELECT current_setting('search_path')")
                        .getSingleResult()));

        assertThat(insideB).isEqualTo(schemaOf(TestTenants.B) + ", extensions");
        assertThat(searchPath(searchPathAndBackend(pool))).isEqualTo(NO_TENANT);
    }

    @Test
    void jdbcWithoutATenantScopeFailsClosed_criterion17() {
        assertThatThrownBy(tenantData::getConnection).isInstanceOf(NoTenantInScopeException.class);
    }

    @Test
    void hibernateWithoutATenantScopeFailsClosed_criterion17() {
        assertThatThrownBy(() -> transactions.execute(status ->
                currentEntityManager().createNativeQuery("SELECT 1").getSingleResult()))
                .hasRootCauseInstanceOf(NoTenantInScopeException.class);
    }

    @Test
    void theConnectionProviderOnlyAcceptsTenantSchemaNames() {
        SchemaPerTenantConnectionProvider provider = new SchemaPerTenantConnectionProvider(pool);

        assertThatIllegalArgumentException().isThrownBy(() -> provider.getConnection("public"));
        assertThatIllegalArgumentException().isThrownBy(() -> provider.getConnection(NO_TENANT));
        assertThatIllegalArgumentException().isThrownBy(() -> provider.getConnection("global"));
    }

    @Test
    void anEntityOfTenantAIsInvisibleFromTenantB_criteria4And6() {
        createProbeTable(TestTenants.A);
        createProbeTable(TestTenants.B);
        ProbeNote note = new ProbeNote(UUID.randomUUID(), "only in A");

        inTransactionOf(TestTenants.A, entityManager -> {
            entityManager.persist(note);
            return note;
        });

        assertThat(findProbe(TestTenants.B, note.id())).isNull();
        assertThat(findProbe(TestTenants.A, note.id()).text()).isEqualTo("only in A");
    }

    @Test
    void jdbcAndHibernateShareTheTenantConnectionInsideATransaction() {
        createProbeTable(TestTenants.A);
        UUID id = UUID.randomUUID();

        Long seenByJdbc = inTransactionOf(TestTenants.A, entityManager -> {
            entityManager.persist(new ProbeNote(id, "flushed"));
            entityManager.flush();
            return JdbcClient.create(tenantData).sql("SELECT count(*) FROM " + ProbeNote.TABLE + " WHERE id = ?")
                    .param(id).query(Long.class).single();
        });

        assertThat(seenByJdbc).isOne();
    }

    private <T> T inTransactionOf(TenantId tenant, Function<EntityManager, T> work) {
        return TenantScope.call(tenant, () -> transactions.execute(status -> work.apply(currentEntityManager())));
    }

    private ProbeNote findProbe(TenantId tenant, UUID id) {
        return inTransactionOf(tenant, entityManager -> entityManager.find(ProbeNote.class, id));
    }

    private void createProbeTable(TenantId tenant) {
        TenantScope.run(tenant, () -> JdbcClient.create(tenantData).sql(ProbeNote.CREATE_TABLE).update());
    }

    private EntityManager currentEntityManager() {
        return EntityManagerFactoryUtils.getTransactionalEntityManager(tenantUnit);
    }

    private static String schemaOf(TenantId tenant) {
        return TestTenants.schemaOf(tenant);
    }

    private static String searchPathAndBackend(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery(SEARCH_PATH_AND_BACKEND)) {
            row.next();
            return row.getString(1);
        }
    }

    private static String searchPath(String searchPathAndBackend) {
        return searchPathAndBackend.substring(0, searchPathAndBackend.lastIndexOf('@'));
    }

    private static String backend(String searchPathAndBackend) {
        return searchPathAndBackend.substring(searchPathAndBackend.lastIndexOf('@') + 1);
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
