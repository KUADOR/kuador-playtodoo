package com.kuador.platodo.api.bookings.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.BookingCancellation;
import com.kuador.platodo.api.bookings.domain.BookingHolder;
import com.kuador.platodo.api.bookings.domain.BookingKind;
import com.kuador.platodo.api.bookings.domain.BookingOverlapException;
import com.kuador.platodo.api.bookings.domain.BookingStatus;
import com.kuador.platodo.api.bookings.domain.TimePeriod;

/**
 * PostgreSQL adapter of {@link BookingRepository}. Table names are unqualified: the tenant schema
 * comes from the connection search_path set by Tenancy ({@code <tenant schema>, extensions}).
 */
public class PostgresBookingRepository implements BookingRepository {

    /** SQLSTATE of an exclusion constraint violation (booking_no_overlap). */
    static final String EXCLUSION_VIOLATION = "23P01";

    /**
     * SQLSTATE of a deadlock. Concurrent inserts of overlapping periods check the exclusion constraint against
     * each other's uncommitted rows and can wait on each other; PostgreSQL then aborts one of them with this
     * code instead of 23P01. A single-row insert waits only on those checks, so the aborted insert lost the
     * slot to a concurrent overlapping booking that goes on to commit.
     */
    static final String DEADLOCK_DETECTED = "40P01";

    private static final String INSERT = """
            INSERT INTO booking (id, court_id, kind, status, period, player_id, holder_name, holder_phone,
                                 block_reason, created_by, created_at, expires_at, confirmed_at,
                                 cancelled_at, cancelled_by)
            VALUES (:id, :courtId, :kind, :status, tstzrange(:periodStart, :periodEnd, '[)'), :playerId,
                    :holderName, :holderPhone, :blockReason, :createdBy, :createdAt, :expiresAt, :confirmedAt,
                    :cancelledAt, :cancelledBy)
            """;

    private static final String BOOKING_COLUMNS = """
            id, court_id, kind, status, lower(period) AS period_start, upper(period) AS period_end,
            player_id, holder_name, holder_phone, block_reason, created_by, created_at, expires_at,
            confirmed_at, cancelled_at, cancelled_by""";

    /** Rows that occupy their slot at :now: confirmed, or pending with a hold that has not ended (ADR-006). */
    private static final String STILL_OCCUPYING =
            "status IN ('PENDING', 'CONFIRMED') AND (status = 'CONFIRMED' OR expires_at > :now)";

    private static final String OVERLAPPING = "period && tstzrange(:periodStart, :periodEnd, '[)')";

    private static final String SELECT_BY_ID = "SELECT %s FROM booking WHERE id = :id".formatted(BOOKING_COLUMNS);

    /** Transaction-scoped advisory lock per tenant schema and court, so tenants never wait for each other. */
    private static final String LOCK_COURT_WRITES = """
            SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(current_schema() || ':' || :courtId, 0)))
                AS court_lock
            """;

    private static final String EXPIRE_OVERDUE_OVERLAPPING = """
            UPDATE booking SET status = 'EXPIRED', version = version + 1
            WHERE court_id = :courtId AND status = 'PENDING' AND expires_at <= :now AND %s
            """.formatted(OVERLAPPING);

    private static final String CONFIRM_IF_PENDING = """
            UPDATE booking SET status = 'CONFIRMED', confirmed_at = :now, version = version + 1
            WHERE id = :id AND player_id = :playerId AND status = 'PENDING' AND expires_at > :now
            RETURNING %s
            """.formatted(BOOKING_COLUMNS);

    private static final String CANCEL_IF_PENDING = """
            UPDATE booking SET status = 'CANCELLED', cancelled_at = :now, cancelled_by = :playerId,
                               version = version + 1
            WHERE id = :id AND player_id = :playerId AND status = 'PENDING' AND expires_at > :now
            RETURNING %s
            """.formatted(BOOKING_COLUMNS);

    private static final String CANCEL_IF_ACTIVE = """
            UPDATE booking SET status = 'CANCELLED', cancelled_at = :now, cancelled_by = :cancelledBy,
                               version = version + 1
            WHERE id = :id AND %s
            RETURNING %s
            """.formatted(STILL_OCCUPYING, BOOKING_COLUMNS);

    private static final String SELECT_ACTIVE_RESERVATION_OVERLAPPING = """
            SELECT %s FROM booking
            WHERE court_id = :courtId AND kind = 'RESERVATION' AND %s AND %s
            ORDER BY lower(period)
            LIMIT 1
            """.formatted(BOOKING_COLUMNS, OVERLAPPING, STILL_OCCUPYING);

    private static final String SELECT_ACTIVE_OVERLAPPING = """
            SELECT %s FROM booking
            WHERE %s AND %s
            ORDER BY lower(period), court_id
            """.formatted(BOOKING_COLUMNS, OVERLAPPING, STILL_OCCUPYING);

    private static final String SELECT_ACTIVE_OF_COURT_OVERLAPPING = """
            SELECT %s FROM booking
            WHERE court_id = :courtId AND %s AND %s
            ORDER BY lower(period)
            """.formatted(BOOKING_COLUMNS, OVERLAPPING, STILL_OCCUPYING);

    private static final String SELECT_ACTIVE_OF_PLAYER_OVERLAPPING = """
            SELECT %s FROM booking
            WHERE player_id = :playerId AND %s AND %s
            ORDER BY lower(period)
            LIMIT 1
            """.formatted(BOOKING_COLUMNS, OVERLAPPING, STILL_OCCUPYING);

    private static final String SELECT_OCCUPIED_PERIODS = """
            SELECT lower(period) AS period_start, upper(period) AS period_end FROM booking
            WHERE court_id = :courtId AND %s AND %s
            ORDER BY lower(period)
            """.formatted(OVERLAPPING, STILL_OCCUPYING);

    /**
     * SKIP LOCKED leaves rows that a concurrent expiry or confirmation is changing to that transaction, and
     * the outer status check makes the update a no-op on rows that stopped being pending meanwhile.
     */
    private static final String EXPIRE_DUE_PENDING = """
            UPDATE booking SET status = 'EXPIRED'
            WHERE status = 'PENDING'
              AND id IN (SELECT id FROM booking
                         WHERE status = 'PENDING' AND expires_at <= :now
                         ORDER BY expires_at
                         LIMIT :limit
                         FOR UPDATE SKIP LOCKED)
            RETURNING\s""" + BOOKING_COLUMNS;

    private final JdbcClient jdbcClient;

    public PostgresBookingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void add(Booking booking) {
        try {
            insert(booking);
        } catch (DataAccessException error) {
            if (lostTheSlotToAnOverlappingBooking(error)) {
                throw new BookingOverlapException(booking.courtId(), booking.period(), error);
            }
            throw error;
        }
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jdbcClient.sql(SELECT_BY_ID)
                .param("id", id)
                .query((row, rowNumber) -> toBooking(row))
                .optional();
    }

    @Override
    public void serializeWritesOfCourt(UUID courtId) {
        jdbcClient.sql(LOCK_COURT_WRITES)
                .param("courtId", courtId.toString())
                .query(Integer.class)
                .single();
    }

    @Override
    public int expireOverdueOverlapping(UUID courtId, TimePeriod period, Instant now) {
        return jdbcClient.sql(EXPIRE_OVERDUE_OVERLAPPING)
                .param("courtId", courtId)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(period.start()))
                .param("periodEnd", toTimestamp(period.end()))
                .update();
    }

    @Override
    public Optional<Booking> confirmIfPending(UUID id, UUID playerId, Instant now) {
        return updateReturning(CONFIRM_IF_PENDING, id, playerId, now);
    }

    @Override
    public Optional<Booking> cancelIfPending(UUID id, UUID playerId, Instant now) {
        return updateReturning(CANCEL_IF_PENDING, id, playerId, now);
    }

    @Override
    public Optional<Booking> cancelIfActive(UUID id, UUID cancelledBy, Instant now) {
        return jdbcClient.sql(CANCEL_IF_ACTIVE)
                .param("id", id)
                .param("cancelledBy", cancelledBy)
                .param("now", toTimestamp(now))
                .query((row, rowNumber) -> toBooking(row))
                .optional();
    }

    @Override
    public Optional<Booking> findActiveReservationOverlapping(UUID courtId, TimePeriod period, Instant now) {
        return jdbcClient.sql(SELECT_ACTIVE_RESERVATION_OVERLAPPING)
                .param("courtId", courtId)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(period.start()))
                .param("periodEnd", toTimestamp(period.end()))
                .query((row, rowNumber) -> toBooking(row))
                .optional();
    }

    @Override
    public List<Booking> findActiveOverlapping(TimePeriod range, Instant now) {
        return jdbcClient.sql(SELECT_ACTIVE_OVERLAPPING)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(range.start()))
                .param("periodEnd", toTimestamp(range.end()))
                .query((row, rowNumber) -> toBooking(row))
                .list();
    }

    @Override
    public List<Booking> findActiveOfCourtOverlapping(UUID courtId, TimePeriod range, Instant now) {
        return jdbcClient.sql(SELECT_ACTIVE_OF_COURT_OVERLAPPING)
                .param("courtId", courtId)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(range.start()))
                .param("periodEnd", toTimestamp(range.end()))
                .query((row, rowNumber) -> toBooking(row))
                .list();
    }

    @Override
    public Optional<Booking> findActiveOfPlayerOverlapping(UUID playerId, TimePeriod period, Instant now) {
        return jdbcClient.sql(SELECT_ACTIVE_OF_PLAYER_OVERLAPPING)
                .param("playerId", playerId)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(period.start()))
                .param("periodEnd", toTimestamp(period.end()))
                .query((row, rowNumber) -> toBooking(row))
                .optional();
    }

    @Override
    public List<TimePeriod> findOccupiedPeriods(UUID courtId, TimePeriod range, Instant now) {
        return jdbcClient.sql(SELECT_OCCUPIED_PERIODS)
                .param("courtId", courtId)
                .param("now", toTimestamp(now))
                .param("periodStart", toTimestamp(range.start()))
                .param("periodEnd", toTimestamp(range.end()))
                .query((row, rowNumber) -> toPeriod(row))
                .list();
    }

    private Optional<Booking> updateReturning(String sql, UUID id, UUID playerId, Instant now) {
        return jdbcClient.sql(sql)
                .param("id", id)
                .param("playerId", playerId)
                .param("now", toTimestamp(now))
                .query((row, rowNumber) -> toBooking(row))
                .optional();
    }

    @Override
    public List<Booking> expirePendingDueBy(Instant now, int limit) {
        return jdbcClient.sql(EXPIRE_DUE_PENDING)
                .param("now", toTimestamp(now))
                .param("limit", limit)
                .query((row, rowNumber) -> toBooking(row))
                .list();
    }

    private void insert(Booking booking) {
        BookingHolder holder = booking.holder();
        BookingCancellation cancellation = booking.cancellation();
        jdbcClient.sql(INSERT)
                .param("id", booking.id())
                .param("courtId", booking.courtId())
                .param("kind", booking.kind().name())
                .param("status", booking.status().name())
                .param("periodStart", toTimestamp(booking.period().start()))
                .param("periodEnd", toTimestamp(booking.period().end()))
                .param("playerId", booking.playerId())
                .param("holderName", holder == null ? null : holder.name())
                .param("holderPhone", holder == null ? null : holder.phone())
                .param("blockReason", booking.blockReason())
                .param("createdBy", booking.createdBy())
                .param("createdAt", toTimestamp(booking.createdAt()))
                .param("expiresAt", toTimestamp(booking.expiresAt()))
                .param("confirmedAt", toTimestamp(booking.confirmedAt()))
                .param("cancelledAt", cancellation == null ? null : toTimestamp(cancellation.cancelledAt()))
                .param("cancelledBy", cancellation == null ? null : cancellation.cancelledBy())
                .update();
    }

    private static boolean lostTheSlotToAnOverlappingBooking(DataAccessException error) {
        return error.getMostSpecificCause() instanceof SQLException sqlError
                && (EXCLUSION_VIOLATION.equals(sqlError.getSQLState())
                        || DEADLOCK_DETECTED.equals(sqlError.getSQLState()));
    }

    private static Booking toBooking(ResultSet row) throws SQLException {
        String holderName = row.getString("holder_name");
        Instant cancelledAt = toInstant(row, "cancelled_at");
        return new Booking(
                row.getObject("id", UUID.class),
                row.getObject("court_id", UUID.class),
                BookingKind.valueOf(row.getString("kind")),
                BookingStatus.valueOf(row.getString("status")),
                toPeriod(row),
                row.getObject("player_id", UUID.class),
                holderName == null ? null : new BookingHolder(holderName, row.getString("holder_phone")),
                row.getString("block_reason"),
                row.getObject("created_by", UUID.class),
                toInstant(row, "created_at"),
                toInstant(row, "expires_at"),
                toInstant(row, "confirmed_at"),
                cancelledAt == null ? null
                        : new BookingCancellation(cancelledAt, row.getObject("cancelled_by", UUID.class)));
    }

    private static TimePeriod toPeriod(ResultSet row) throws SQLException {
        return new TimePeriod(toInstant(row, "period_start"), toInstant(row, "period_end"));
    }

    /** PgJDBC binds {@link OffsetDateTime} as timestamptz; {@link Instant} is not a supported JDBC type. */
    private static OffsetDateTime toTimestamp(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant toInstant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
