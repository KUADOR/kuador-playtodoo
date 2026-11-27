package com.kuador.platodo.api.complexes.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kuador.platodo.api.complexes.application.port.out.ComplexCatalog;
import com.kuador.platodo.api.complexes.domain.CatalogEntry;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** ADR-014: only an active complex is visible in the catalog. */
class SyncComplexCatalogTest {

    private static final UUID ID = UUID.fromString("5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c");
    private static final TenantId TENANT = new TenantId(ID.toString());
    private static final Instant AT = Instant.parse("2026-12-01T14:00:00Z");

    private final RecordingCatalog catalog = new RecordingCatalog();
    private final SyncComplexCatalog sync = new SyncComplexCatalog(catalog, new OneComplexDirectory());

    @Test
    void anActiveComplexIsShownWithItsPublicData() {
        sync.statusChanged(TENANT, TenantStatus.ACTIVE, AT);

        assertThat(catalog.calls).containsExactly("show " + new CatalogEntry(ID, "canchas-el-sol", "Canchas El Sol"));
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherStatusHidesTheComplex(TenantStatus status) {
        sync.statusChanged(TENANT, status, AT);

        assertThat(catalog.calls).containsExactly("hide " + ID);
    }

    @Test
    void aNewSubdomainMovesTheEntry() {
        sync.subdomainChanged(TENANT, "el-sol", AT);

        assertThat(catalog.calls).containsExactly("subdomain " + ID + " el-sol");
    }

    /** ADR-016: «borrar todo» includes the catalog row of the purged complex. */
    @Test
    void aPurgedComplexLeavesTheCatalog() {
        sync.purged(TENANT);

        assertThat(catalog.calls).containsExactly("remove " + ID);
    }

    private static final class RecordingCatalog implements ComplexCatalog {

        private final List<String> calls = new ArrayList<>();

        @Override
        public void show(CatalogEntry entry, Instant at) {
            calls.add("show " + entry);
        }

        @Override
        public void hide(UUID tenantId, Instant at) {
            calls.add("hide " + tenantId);
        }

        @Override
        public void changeSubdomain(UUID tenantId, String subdomain, Instant at) {
            calls.add("subdomain " + tenantId + " " + subdomain);
        }

        @Override
        public void remove(UUID tenantId) {
            calls.add("remove " + tenantId);
        }
    }

    private static final class OneComplexDirectory implements TenantDirectory {

        @Override
        public Optional<TenantEntry> find(TenantId tenant) {
            return Optional.empty();
        }

        @Override
        public List<TenantProfile> profiles(Collection<TenantId> tenants) {
            return List.of(new TenantProfile(TENANT, "Canchas El Sol", TenantStatus.ACTIVE, "canchas-el-sol", null,
                    AT, null, null, null, null, null, 1));
        }
    }
}
