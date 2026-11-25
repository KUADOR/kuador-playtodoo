package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

class TenantMigrationsTest {

    private static final TenantId FIRST = new TenantId("00000000-0000-4000-8000-000000000001");
    private static final TenantId FAILING = new TenantId("00000000-0000-4000-8000-000000000002");
    private static final TenantId LAST = new TenantId("00000000-0000-4000-8000-000000000003");

    private final RecordingMigrator migrator = new RecordingMigrator();

    @Test
    void migratesEveryTenantOneAtATimeEachInsideItsOwnScope() {
        new TenantMigrations(() -> List.of(FIRST, LAST), migrator).migrateAll();

        assertThat(migrator.migrated).containsExactly(
                new Migration(TenantSchemaName.of(FIRST).value(), Optional.of(FIRST)),
                new Migration(TenantSchemaName.of(LAST).value(), Optional.of(LAST)));
    }

    @Test
    void stopsAtTheFirstFailingTenantAndNamesItsSchema() {
        TenantMigrations migrations = new TenantMigrations(() -> List.of(FIRST, FAILING, LAST), migrator);

        assertThatThrownBy(migrations::migrateAll)
                .isInstanceOf(TenantMigrationException.class)
                .hasMessageContaining(TenantSchemaName.of(FAILING).value());
        assertThat(migrator.migrated).extracting(Migration::schema)
                .containsExactly(TenantSchemaName.of(FIRST).value());
    }

    private record Migration(String schema, Optional<TenantId> scope) {
    }

    private static final class RecordingMigrator extends TenantSchemaMigrator {

        private final List<Migration> migrated = new ArrayList<>();

        private RecordingMigrator() {
            super(null);
        }

        @Override
        public void migrate(String tenantSchema) {
            if (tenantSchema.equals(TenantSchemaName.of(FAILING).value())) {
                throw new IllegalStateException("migration failed");
            }
            migrated.add(new Migration(tenantSchema, TenantScope.current()));
        }
    }
}
