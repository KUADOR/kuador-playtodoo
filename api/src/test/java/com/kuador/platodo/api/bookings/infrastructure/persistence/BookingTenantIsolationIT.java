package com.kuador.platodo.api.bookings.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.bookings.domain.Booking;
import com.kuador.platodo.api.bookings.domain.TimePeriod;
import com.kuador.platodo.api.support.CoversTenantEntity;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.ScopedTenantDataSource;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * The booking adapter on the production tenant data source: what tenant A writes is neither read nor changed from
 * tenant B (criteria 4 to 6 of PLATODO-26).
 */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
@CoversTenantEntity("booking")
class BookingTenantIsolationIT {

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final TimePeriod EIGHT_TO_NINE =
            new TimePeriod(Instant.parse("2026-12-04T20:00:00Z"), Instant.parse("2026-12-04T21:00:00Z"));

    @Autowired
    private ScopedTenantDataSource tenantData;
    @Autowired
    private TenantMigrations migrations;

    private PostgresBookingRepository bookings;
    private final UUID courtId = UUID.randomUUID();

    @BeforeEach
    void provisionTwoTenants() {
        TestTenants.provision(migrations, TestTenants.A);
        TestTenants.provision(migrations, TestTenants.B);
        bookings = new PostgresBookingRepository(JdbcClient.create(tenantData));
    }

    @Test
    void aBookingOfTenantAIsNotFoundFromTenantB() {
        Booking booking = pendingOnTheCourt();

        TenantScope.run(TestTenants.A, () -> bookings.add(booking));

        assertThat(TenantScope.call(TestTenants.B, () -> bookings.findById(booking.id()))).isEmpty();
        assertThat(TenantScope.call(TestTenants.A, () -> bookings.findById(booking.id()))).contains(booking);
    }

    @Test
    void tenantBCannotCancelABookingOfTenantA() {
        Booking booking = pendingOnTheCourt();
        TenantScope.run(TestTenants.A, () -> bookings.add(booking));

        assertThat(TenantScope.call(TestTenants.B, () -> bookings.cancelIfActive(booking.id(), UUID.randomUUID(), NOW)))
                .isEmpty();
        assertThat(TenantScope.call(TestTenants.A, () -> bookings.findById(booking.id())))
                .get().extracting(Booking::status).isEqualTo(booking.status());
    }

    @Test
    void bookingsOfTenantADoNotBlockTheSameSlotInTenantB() {
        TenantScope.run(TestTenants.A, () -> bookings.add(pendingOnTheCourt()));
        Booking inB = pendingOnTheCourt();

        TenantScope.run(TestTenants.B, () -> bookings.add(inB));

        assertThat(TenantScope.call(TestTenants.B, () -> bookings.findById(inB.id()))).contains(inB);
    }

    private Booking pendingOnTheCourt() {
        return Booking.pendingReservation(UUID.randomUUID(), courtId, EIGHT_TO_NINE, UUID.randomUUID(), NOW);
    }
}
