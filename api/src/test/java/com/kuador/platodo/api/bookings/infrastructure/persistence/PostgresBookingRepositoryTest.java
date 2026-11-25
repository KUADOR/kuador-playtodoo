package com.kuador.platodo.api.bookings.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/** How the adapter translates database errors of an insert, without a database. */
class PostgresBookingRepositoryTest {

    private static final Booking PENDING = Booking.pendingReservation(UUID.randomUUID(), UUID.randomUUID(),
            new TimePeriod(Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z")),
            UUID.randomUUID(), Instant.parse("2026-12-04T18:00:00Z"));

    @ParameterizedTest
    @ValueSource(strings = {
            PostgresBookingRepository.EXCLUSION_VIOLATION,
            PostgresBookingRepository.DEADLOCK_DETECTED})
    void anInsertThatLosesTheSlotToAConcurrentOneIsAnOverlap(String sqlState) {
        PostgresBookingRepository repository = repositoryWhoseInsertFailsWith(sqlState);

        assertThatThrownBy(() -> repository.add(PENDING)).isInstanceOf(BookingOverlapException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"23514", "23505"})
    void otherDatabaseErrorsAreNotOverlaps(String sqlState) {
        PostgresBookingRepository repository = repositoryWhoseInsertFailsWith(sqlState);

        assertThatThrownBy(() -> repository.add(PENDING))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(BookingOverlapException.class);
    }

    private static PostgresBookingRepository repositoryWhoseInsertFailsWith(String sqlState) {
        JdbcClient jdbcClient = mock(JdbcClient.class);
        JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
        when(jdbcClient.sql(anyString())).thenReturn(statement);
        when(statement.update()).thenThrow(new DataIntegrityViolationException("insert failed",
                new SQLException("database error", sqlState)));
        return new PostgresBookingRepository(jdbcClient);
    }
}
