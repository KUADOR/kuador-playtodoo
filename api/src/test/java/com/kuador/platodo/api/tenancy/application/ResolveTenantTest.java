package com.kuador.platodo.api.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

class ResolveTenantTest {

    private static final TenantId TENANT = new TenantId("5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c");
    private static final String NAME = "Canchas El Sol";

    private final RecordingDomains domains = new RecordingDomains();
    private final ResolveTenant resolveTenant = new ResolveTenant(domains);

    @Test
    void anActiveComplexIsResolved_criterion1() {
        domains.add("elsol", TenantStatus.ACTIVE);

        assertThat(resolveTenant.resolve("ElSol")).isEqualTo(new TenantResolution.Active(TENANT, NAME));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void aSuspendedOrArchivedComplexIsUnavailable_criterion3(TenantStatus status) {
        domains.add("elsol", status);

        assertThat(resolveTenant.resolve("elsol")).isEqualTo(new TenantResolution.Unavailable());
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"PROVISIONING", "PROVISIONING_FAILED", "PURGING", "PURGED"})
    void aComplexThatIsNotOperatingIsNotFound(TenantStatus status) {
        domains.add("elsol", status);

        assertThat(resolveTenant.resolve("elsol")).isEqualTo(new TenantResolution.NotFound());
    }

    @Test
    void anUnknownSubdomainIsNotFound_criterion2() {
        assertThat(resolveTenant.resolve("nobody")).isEqualTo(new TenantResolution.NotFound());
        assertThat(domains.lookups).containsExactly("nobody");
    }

    @Test
    void aMalformedSubdomainIsNotFoundWithoutAnyLookup_criterion2() {
        assertThat(resolveTenant.resolve("xn--elsol")).isEqualTo(new TenantResolution.NotFound());
        assertThat(domains.lookups).isEmpty();
    }

    private static final class RecordingDomains implements TenantDomains {

        private final Map<String, DomainOwner> owners = new HashMap<>();
        private final List<String> lookups = new ArrayList<>();

        void add(String subdomain, TenantStatus status) {
            owners.put(subdomain, new DomainOwner(TENANT, NAME, status));
        }

        @Override
        public Optional<DomainOwner> findByCurrentSubdomain(Subdomain subdomain) {
            lookups.add(subdomain.value());
            return Optional.ofNullable(owners.get(subdomain.value()));
        }
    }
}
