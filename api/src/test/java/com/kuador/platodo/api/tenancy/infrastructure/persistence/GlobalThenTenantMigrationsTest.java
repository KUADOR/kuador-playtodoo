package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class GlobalThenTenantMigrationsTest {

    private final Flyway global = mock(Flyway.class);
    private final TenantMigrations tenants = mock(TenantMigrations.class);
    private final GlobalThenTenantMigrations strategy = new GlobalThenTenantMigrations(tenants);

    @Test
    void migratesTheGlobalSchemaBeforeTheTenantSchemas() {
        strategy.migrate(global);

        InOrder order = inOrder(global, tenants);
        order.verify(global).migrate();
        order.verify(tenants).migrateAll();
    }

    @Test
    void aFailedGlobalMigrationStopsTheStartupBeforeAnyTenant() {
        doThrow(new FlywayException("global failed")).when(global).migrate();

        assertThatThrownBy(() -> strategy.migrate(global)).isInstanceOf(FlywayException.class);
        verifyNoInteractions(tenants);
    }
}
