package com.kuador.platodo.api.tenancy.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.support.TenantDatabase;

class TenantSchemaMigratorIT {

    @Test
    void createsTheBookingTableWithItsConstraintsAndIndexesInTheTenantSchema() {
        JdbcClient jdbc = JdbcClient.create(TenantDatabase.migratedTenant("tenant_migrated"));

        List<String> constraints = jdbc.sql("""
                        SELECT conname FROM pg_constraint
                        WHERE conrelid = 'tenant_migrated.booking'::regclass AND contype IN ('c', 'x')
                        """)
                .query(String.class).list();
        List<String> indexes = jdbc.sql("SELECT indexname FROM pg_indexes WHERE schemaname = 'tenant_migrated'")
                .query(String.class).list();

        assertThat(constraints).contains(
                "booking_no_overlap", "booking_pending_has_expiry", "booking_block_never_pending");
        assertThat(indexes).contains("booking_pending_expiry_idx", "booking_active_player_idx");
    }

    @Test
    void keepsTheMigrationHistoryInsideTheTenantSchema() {
        JdbcClient jdbc = JdbcClient.create(TenantDatabase.migratedTenant("tenant_history"));

        Integer applied = jdbc.sql("SELECT count(*) FROM tenant_history.flyway_schema_history WHERE success")
                .query(Integer.class).single();

        assertThat(applied).isPositive();
    }

    @Test
    void migratingAnUpToDateTenantAgainChangesNothing() {
        DataSource dataSource = TenantDatabase.migratedTenant("tenant_again");
        TenantSchemaMigrator migrator = new TenantSchemaMigrator(dataSource);

        assertThatNoException().isThrownBy(() -> migrator.migrate("tenant_again"));
    }
}
