package com.kuador.platodo.api.bookings.infrastructure.config;

import java.time.Clock;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import com.kuador.platodo.api.bookings.application.BlockSlot;
import com.kuador.platodo.api.bookings.application.CancelBookingByStaff;
import com.kuador.platodo.api.bookings.application.CreateStaffBooking;
import com.kuador.platodo.api.bookings.application.GetStaffBooking;
import com.kuador.platodo.api.bookings.application.ListStaffBookings;
import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.bookings.infrastructure.failclosed.UnconfiguredCourtSlotRules;
import com.kuador.platodo.api.bookings.infrastructure.identity.PrincipalCurrentStaff;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnStaffApiEnabled;
import com.kuador.platodo.api.tenancy.application.port.in.SupportGrants;

/**
 * Wires the staff booking use cases on the adapters of {@link BookingsConfiguration}. The staff member of each
 * request comes from the access token accepted by Identity and Access ({@code PrincipalCurrentStaff}); a
 * {@link CurrentStaff} bean, if one is registered (tests), replaces it. Until the Courts module registers its
 * {@link CourtSlotRules}, a fail-closed default reports every court as not found.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnStaffApiEnabled
class StaffBookingConfiguration {

    private final BookingRepository bookings;
    private final BookingEventPublisher events;
    private final CourtSlotRules courtSlotRules;
    private final CurrentStaff currentStaff;
    private final TransactionOperations transactions;
    private final Clock clock;

    StaffBookingConfiguration(DataSource dataSource, BookingRepository bookings, BookingEventPublisher events,
            ObjectProvider<CourtSlotRules> courtSlotRules, ObjectProvider<CurrentStaff> currentStaff,
            SupportGrants supportGrants, ObjectProvider<PlatformTransactionManager> transactionManager,
            ObjectProvider<Clock> clock) {
        this.bookings = bookings;
        this.events = events;
        this.courtSlotRules = courtSlotRules.getIfAvailable(UnconfiguredCourtSlotRules::new);
        this.currentStaff = currentStaff.getIfAvailable(() -> new PrincipalCurrentStaff(supportGrants));
        this.transactions = new TransactionTemplate(
                transactionManager.getIfAvailable(() -> new DataSourceTransactionManager(dataSource)));
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    ListStaffBookings listStaffBookings() {
        return new ListStaffBookings(bookings, currentStaff, clock);
    }

    @Bean
    GetStaffBooking getStaffBooking() {
        return new GetStaffBooking(bookings, currentStaff, clock);
    }

    @Bean
    CreateStaffBooking createStaffBooking() {
        return new CreateStaffBooking(bookings, courtSlotRules, currentStaff, events, transactions, clock);
    }

    @Bean
    BlockSlot blockSlot() {
        return new BlockSlot(bookings, courtSlotRules, currentStaff, events, transactions, clock);
    }

    @Bean
    CancelBookingByStaff cancelBookingByStaff() {
        return new CancelBookingByStaff(bookings, currentStaff, events, transactions, clock);
    }
}
