package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * The two persistence units of ADR-010 (D1) over one connection pool (D2).
 * <ul>
 *   <li><b>Global</b>: plain JPA on the {@code global} schema. Its entities live in
 *       {@value #GLOBAL_ENTITY_PACKAGES}.</li>
 *   <li><b>Tenant</b>: Hibernate multi-tenancy by schema, resolved from {@code TenantScope}. Its entities live in
 *       {@value #TENANT_ENTITY_PACKAGES}. Plain JDBC adapters of tenant modules inject the only autowirable
 *       {@link DataSource}, the {@link ScopedTenantDataSource}, and share the tenant transaction manager.</li>
 * </ul>
 * The raw pool is not autowirable by type: only Tenancy and the global unit reach it, through
 * {@value #CONNECTION_POOL}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
class PersistenceConfiguration {

    static final String CONNECTION_POOL = "connectionPool";
    static final String GLOBAL_ENTITY_PACKAGES = "com.kuador.platodo.api.*.infrastructure.persistence.global";
    static final String TENANT_ENTITY_PACKAGES = "com.kuador.platodo.api.*.infrastructure.persistence.tenant";

    private static final String GLOBAL_SCHEMA = "global";
    private static final String GLOBAL_UNIT = "global";
    private static final String TENANT_UNIT = "tenant";
    private static final String HIKARI_PROPERTIES = "spring.datasource.hikari";
    private static final String NO_SCHEMA_GENERATION = "none";

    /**
     * The shared pool, configured by {@code spring.datasource.*}. New connections start at {@code no_tenant},
     * whatever the configuration says.
     */
    @Bean(name = CONNECTION_POOL, defaultCandidate = false)
    @Qualifier(CONNECTION_POOL)
    @FlywayDataSource
    HikariDataSource connectionPool(DataSourceProperties properties, Environment environment) {
        HikariDataSource pool = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        Binder.get(environment).bind(HIKARI_PROPERTIES, Bindable.ofInstance(pool));
        pool.setConnectionInitSql(TenantSearchPath.RESET_SQL);
        return pool;
    }

    @Bean
    @Primary
    ScopedTenantDataSource tenantDataSource(@Qualifier(CONNECTION_POOL) DataSource pool) {
        return new ScopedTenantDataSource(pool);
    }

    @Bean
    LocalContainerEntityManagerFactoryBean globalEntityManagerFactory(@Qualifier(CONNECTION_POOL) DataSource pool) {
        LocalContainerEntityManagerFactoryBean factory = entityManagerFactory(GLOBAL_UNIT, GLOBAL_ENTITY_PACKAGES);
        factory.setDataSource(pool);
        factory.setJpaPropertyMap(Map.of(
                MappingSettings.DEFAULT_SCHEMA, GLOBAL_SCHEMA,
                SchemaToolingSettings.HBM2DDL_AUTO, NO_SCHEMA_GENERATION));
        return factory;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(@Qualifier(CONNECTION_POOL) DataSource pool) {
        LocalContainerEntityManagerFactoryBean factory = entityManagerFactory(TENANT_UNIT, TENANT_ENTITY_PACKAGES);
        factory.setJpaPropertyMap(Map.of(
                MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, new SchemaPerTenantConnectionProvider(pool),
                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new ScopedTenantIdentifierResolver(),
                SchemaToolingSettings.HBM2DDL_AUTO, NO_SCHEMA_GENERATION));
        return factory;
    }

    @Bean(defaultCandidate = false)
    @Qualifier(GLOBAL_UNIT)
    JpaTransactionManager globalTransactionManager(
            @Qualifier("globalEntityManagerFactory") EntityManagerFactory global,
            @Qualifier(CONNECTION_POOL) DataSource pool) {
        JpaTransactionManager transactions = new JpaTransactionManager(global);
        transactions.setDataSource(pool);
        return transactions;
    }

    /** Also drives plain JDBC on {@link ScopedTenantDataSource}: both share the session connection. */
    @Bean
    @Primary
    JpaTransactionManager transactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory tenant, ScopedTenantDataSource tenantData) {
        JpaTransactionManager transactions = new JpaTransactionManager(tenant);
        transactions.setDataSource(tenantData);
        return transactions;
    }

    @Bean
    TenantMigrations tenantMigrations(
            @Qualifier(CONNECTION_POOL) DataSource pool, ScopedTenantDataSource tenantData) {
        return new TenantMigrations(
                new JdbcMigratableTenants(JdbcClient.create(pool)), new TenantSchemaMigrator(tenantData));
    }

    @Bean
    TenantDomains tenantDomains(@Qualifier(CONNECTION_POOL) DataSource pool) {
        return new JdbcTenantDomains(JdbcClient.create(pool));
    }

    @Bean
    TenantDirectory tenantDirectory(@Qualifier(CONNECTION_POOL) DataSource pool, RequestHosts hosts) {
        return new JdbcTenantDirectory(JdbcClient.create(pool), hosts);
    }

    @Bean
    FlywayMigrationStrategy globalThenTenantMigrations(TenantMigrations tenantMigrations) {
        return new GlobalThenTenantMigrations(tenantMigrations);
    }

    private static LocalContainerEntityManagerFactoryBean entityManagerFactory(String unit, String entityPackages) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName(unit);
        factory.setPackagesToScan(entityPackages);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        return factory;
    }
}
