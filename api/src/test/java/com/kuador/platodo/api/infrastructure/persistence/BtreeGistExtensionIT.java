package com.kuador.platodo.api.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Proves that the shared extensions init script (the same one used by api/compose.yaml)
 * makes btree_gist available to tenant schemas, so a no-overlap EXCLUDE constraint works.
 */
@Testcontainers
class BtreeGistExtensionIT {

    /** Keep in sync with the image in api/compose.yaml. */
    private static final String POSTGRES_IMAGE = "postgres:18-alpine";
    private static final Path EXTENSIONS_SCRIPT = Path.of("docker/postgres/initdb/01-extensions.sql");
    private static final String EXTENSIONS_SCHEMA = "extensions";
    private static final String EXCLUSION_VIOLATION = "23P01";
    private static final String COURT_ID = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(EXTENSIONS_SCRIPT),
                    "/docker-entrypoint-initdb.d/" + EXTENSIONS_SCRIPT.getFileName());

    @Test
    void installsBtreeGistInTheSharedExtensionsSchema() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT n.nspname
                     FROM pg_extension e JOIN pg_namespace n ON n.oid = e.extnamespace
                     WHERE e.extname = 'btree_gist'
                     """)) {
            assertThat(result.next()).as("btree_gist is installed").isTrue();
            assertThat(result.getString(1)).isEqualTo(EXTENSIONS_SCHEMA);
        }
    }

    @Test
    void extensionsScriptIsIdempotent() throws Exception {
        String script = Files.readString(EXTENSIONS_SCRIPT);
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            assertThatNoException().isThrownBy(() -> statement.execute(script));
        }
    }

    @Test
    void tenantSchemaRejectsOverlappingPeriodsForTheSameCourt() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            createTenantWithBookingTable(statement, "tenant_overlap");

            insertBooking(statement, "[2026-12-01 20:00+00, 2026-12-01 21:00+00)");

            assertThatThrownBy(() -> insertBooking(statement, "[2026-12-01 20:30+00, 2026-12-01 21:30+00)"))
                    .isInstanceOfSatisfying(SQLException.class,
                            error -> assertThat(error.getSQLState()).isEqualTo(EXCLUSION_VIOLATION));
        }
    }

    @Test
    void tenantSchemaAcceptsAdjacentPeriodsForTheSameCourt() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            createTenantWithBookingTable(statement, "tenant_adjacent");

            insertBooking(statement, "[2026-12-01 20:00+00, 2026-12-01 21:00+00)");

            assertThatNoException().isThrownBy(
                    () -> insertBooking(statement, "[2026-12-01 21:00+00, 2026-12-01 22:00+00)"));
        }
    }

    @Test
    void sameCourtAndPeriodInAnotherTenantSchemaDoesNotConflict() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            String period = "[2026-12-01 20:00+00, 2026-12-01 21:00+00)";
            createTenantWithBookingTable(statement, "tenant_a");
            insertBooking(statement, period);

            createTenantWithBookingTable(statement, "tenant_b");
            assertThatNoException().isThrownBy(() -> insertBooking(statement, period));

            assertThat(countBookings(statement)).isEqualTo(1);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /** Creates a tenant schema and activates it with the shared extensions schema, as Tenancy will do. */
    private static void createTenantWithBookingTable(Statement statement, String tenantSchema) throws SQLException {
        statement.execute("CREATE SCHEMA " + tenantSchema);
        statement.execute("SET search_path TO " + tenantSchema + ", " + EXTENSIONS_SCHEMA);
        statement.execute("""
                CREATE TABLE booking (
                  id       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                  court_id uuid NOT NULL,
                  period   tstzrange NOT NULL,
                  CONSTRAINT booking_no_overlap EXCLUDE USING gist (court_id WITH =, period WITH &&)
                )
                """);
    }

    private static void insertBooking(Statement statement, String period) throws SQLException {
        statement.executeUpdate(
                "INSERT INTO booking (court_id, period) VALUES ('" + COURT_ID + "', '" + period + "')");
    }

    private static int countBookings(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("SELECT count(*) FROM booking")) {
            result.next();
            return result.getInt(1);
        }
    }
}
