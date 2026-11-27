package com.kuador.platodo.api.complexes.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

import com.kuador.platodo.api.complexes.application.SyncComplexCatalog;
import com.kuador.platodo.api.complexes.application.port.out.ComplexCatalog;
import com.kuador.platodo.api.complexes.infrastructure.events.TenantCatalogListener;
import com.kuador.platodo.api.complexes.infrastructure.persistence.global.JpaComplexCatalog;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;

import jakarta.persistence.EntityManagerFactory;

/** Global catalog of complexes (ADR-008, ADR-014), kept aligned with the lifecycle of Tenancy. */
@Configuration(proxyBeanMethods = false)
class ComplexCatalogConfiguration {

    private static final String GLOBAL_ENTITY_MANAGER_FACTORY = "globalEntityManagerFactory";

    @Bean
    ComplexCatalog complexCatalog(@Qualifier(GLOBAL_ENTITY_MANAGER_FACTORY) EntityManagerFactory global) {
        return new JpaComplexCatalog(SharedEntityManagerCreator.createSharedEntityManager(global));
    }

    @Bean
    TenantCatalogListener tenantCatalogListener(ComplexCatalog catalog, TenantDirectory directory) {
        return new TenantCatalogListener(new SyncComplexCatalog(catalog, directory));
    }
}
