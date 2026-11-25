package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.tenancy.application.port.in.NoTenantInScopeException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

class ScopedTenantIdentifierResolverTest {

    private final ScopedTenantIdentifierResolver resolver = new ScopedTenantIdentifierResolver();

    @Test
    void withoutATenantScopeItThrowsInsteadOfUsingADefaultTenant_criterion17() {
        assertThatThrownBy(resolver::resolveCurrentTenantIdentifier).isInstanceOf(NoTenantInScopeException.class);
    }

    @Test
    void insideATenantScopeItResolvesThatTenantsSchema() {
        TenantId tenant = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");

        String identifier = TenantScope.call(tenant, resolver::resolveCurrentTenantIdentifier);

        assertThat(identifier).isEqualTo("t_0a0a0a0a00004000800000000000000a");
    }

    @Test
    void validatesExistingCurrentSessionsAgainstTheScope() {
        assertThat(resolver.validateExistingCurrentSessions()).isTrue();
    }
}
