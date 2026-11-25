package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;

import com.kuador.platodo.api.tenancy.application.port.out.GlobalTransaction;
import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.application.port.out.TenantRegistry;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;

/** Global-schema adapters of the tenant lifecycle and subdomains (ADR-012, ADR-013). */
@Configuration(proxyBeanMethods = false)
class LifecyclePersistenceConfiguration {

    private static final String GLOBAL_TRANSACTIONS = "global";

    @Bean
    TenantRegistry tenantRegistry(@Qualifier(PersistenceConfiguration.CONNECTION_POOL) DataSource pool) {
        return new JdbcTenantRegistry(JdbcClient.create(pool));
    }

    @Bean
    SubdomainUsage subdomainUsage(@Qualifier(PersistenceConfiguration.CONNECTION_POOL) DataSource pool) {
        return new JdbcSubdomainUsage(JdbcClient.create(pool));
    }

    @Bean
    TenantSchemas tenantSchemas(TenantMigrations migrations) {
        return new MigratingTenantSchemas(migrations);
    }

    @Bean
    GlobalTransaction globalTransaction(@Qualifier(GLOBAL_TRANSACTIONS) PlatformTransactionManager transactions) {
        return new SpringGlobalTransaction(transactions);
    }
}
