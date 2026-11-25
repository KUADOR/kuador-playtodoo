package com.kuador.platodo.api.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.support.CoversTenantEntity;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Meta-test of ADR-010: every table that the tenant migrations create needs a test class annotated with
 * {@link CoversTenantEntity} that proves tenant isolation for it.
 */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class TenantEntityCoverageIT {

    private static final String BASE_PACKAGE = "com.kuador.platodo.api";
    private static final String FLYWAY_HISTORY = "flyway_schema_history";

    @Autowired
    private TenantMigrations migrations;

    @Test
    void everyTenantTableHasAnIsolationTest() {
        assertThat(coveredTables())
                .as("Tables of db/migration/tenant without a test annotated @CoversTenantEntity")
                .containsAll(tenantTables());
    }

    @Test
    void everyCoveredNameIsATenantTable() {
        assertThat(tenantTables())
                .as("Names in @CoversTenantEntity that the tenant migrations do not create")
                .containsAll(coveredTables());
    }

    /** Tables of a freshly migrated tenant schema, so tables created by other tests do not count. */
    private Set<String> tenantTables() {
        TenantId fresh = new TenantId(UUID.randomUUID().toString());
        migrations.migrate(fresh);
        return Set.copyOf(TenantDatabase.admin().sql("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = :schema AND table_name <> :history
                        """)
                .param("schema", TestTenants.schemaOf(fresh))
                .param("history", FLYWAY_HISTORY)
                .query(String.class)
                .list());
    }

    private static Set<String> coveredTables() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE)
                .stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(CoversTenantEntity.class))
                .map(TenantEntityCoverageIT::coveredBy)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());
    }

    private static String[] coveredBy(JavaClass testClass) {
        return testClass.getAnnotationOfType(CoversTenantEntity.class).value();
    }
}
