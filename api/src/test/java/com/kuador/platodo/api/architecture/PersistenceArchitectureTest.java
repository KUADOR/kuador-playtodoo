package com.kuador.platodo.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.kuador.platodo.api.architecture.fixtures.QualifiedSqlRepository;
import com.kuador.platodo.api.architecture.fixtures.SearchPathRepository;
import com.kuador.platodo.api.architecture.fixtures.UnqualifiedSqlRepository;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/** Persistence rules of ADR-010 that a reviewer could miss. */
class PersistenceArchitectureTest {

    private static final String BASE_PACKAGE = "com.kuador.platodo.api";
    /** The only code that may name schemas: Tenancy's persistence, which owns the search_path and global.tenant. */
    private static final String SCHEMA_OWNER = BASE_PACKAGE + ".tenancy.infrastructure.persistence..";

    private static final ArchRule NO_SCHEMA_QUALIFIED_SQL = classes()
            .that().resideOutsideOfPackage(SCHEMA_OWNER)
            .should(SchemaQualifiedSql.notUseSchemaQualifiedSql());

    @Test
    void productionCodeOutsideTenancyNeverNamesASchemaInNativeSql() {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        NO_SCHEMA_QUALIFIED_SQL.check(production);
    }

    @Test
    void theRuleCatchesSchemaQualifiedTablesAndSearchPathChanges() {
        JavaClasses fixtures = new ClassFileImporter()
                .importClasses(QualifiedSqlRepository.class, SearchPathRepository.class);

        assertThat(NO_SCHEMA_QUALIFIED_SQL.evaluate(fixtures).getFailureReport().getDetails())
                .hasSize(2)
                .anySatisfy(line -> assertThat(line).contains("global.tenant"))
                .anySatisfy(line -> assertThat(line).contains("search_path"));
    }

    @Test
    void theRuleAcceptsUnqualifiedSqlWithTableAliases() {
        JavaClasses fixtures = new ClassFileImporter().importClasses(UnqualifiedSqlRepository.class);

        assertThat(NO_SCHEMA_QUALIFIED_SQL.evaluate(fixtures).hasViolation()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM global.tenant", "select id from \"t_0a\".booking", "INSERT INTO other.booking (id) VALUES (?)",
        "UPDATE t_x.booking SET status = ?", "DELETE FROM x.y", "SELECT 1 FROM a JOIN b.c ON true",
        "TRUNCATE TABLE x.y", "SET search_path TO t_x", "SET SCHEMA 'x'"})
    void recognisesSqlThatNamesASchema(String sql) {
        assertThat(SchemaQualifiedSql.namesASchema(sql)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT b.id FROM booking b WHERE b.court_id = ?", "UPDATE booking SET status = 'EXPIRED'",
        "Could not expire pending bookings in tenant {}", "booking.status"})
    void ignoresUnqualifiedSqlAndPlainText(String text) {
        assertThat(SchemaQualifiedSql.namesASchema(text)).isFalse();
    }
}
