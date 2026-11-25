package com.kuador.platodo.api.identity.infrastructure.web;

import static com.kuador.platodo.api.support.TestTenants.FLYWAY_INITIALIZER;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import com.kuador.platodo.api.bookings.support.StubCourtSlotRules;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * Context shared by the identity integration tests, so they reuse one application context (and one connection
 * pool) as long as they also enable both booking APIs: tenants A and B provisioned and courts stubbed. Every other
 * port uses its production adapter, Identity and Access included.
 */
@TestConfiguration(proxyBeanMethods = false)
class TwoComplexesWithBookingApis {

    @Bean
    @DependsOn(FLYWAY_INITIALIZER)
    TenantId[] provisionedTenants(TenantMigrations migrations) {
        return new TenantId[] {
            TestTenants.provision(migrations, TestTenants.A), TestTenants.provision(migrations, TestTenants.B)};
    }

    @Bean
    StubCourtSlotRules courtSlotRules() {
        return new StubCourtSlotRules();
    }
}
