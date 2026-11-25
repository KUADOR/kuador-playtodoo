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

import com.kuador.platodo.api.bookings.application.CancelOwnPendingBooking;
import com.kuador.platodo.api.bookings.application.ConfirmBooking;
import com.kuador.platodo.api.bookings.application.CreatePendingBooking;
import com.kuador.platodo.api.bookings.application.GetBooking;
import com.kuador.platodo.api.bookings.application.GetCourtOccupancy;
import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.application.port.out.CourtSlotRules;
import com.kuador.platodo.api.bookings.application.port.out.CurrentPlayer;
import com.kuador.platodo.api.bookings.infrastructure.failclosed.UnauthenticatedCurrentPlayer;
import com.kuador.platodo.api.bookings.infrastructure.failclosed.UnconfiguredCourtSlotRules;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;

/**
 * Wires the player booking use cases on the adapters of {@link BookingsConfiguration}. Until the Courts and
 * Identity modules register their {@link CourtSlotRules} and {@link CurrentPlayer} beans, fail-closed defaults
 * reject every request.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPlayerApiEnabled
class PlayerBookingConfiguration {

    private final BookingRepository bookings;
    private final CourtSlotRules courtSlotRules;
    private final CurrentPlayer currentPlayer;
    private final TransactionOperations transactions;
    private final Clock clock;

    PlayerBookingConfiguration(DataSource dataSource, BookingRepository bookings,
            ObjectProvider<CourtSlotRules> courtSlotRules, ObjectProvider<CurrentPlayer> currentPlayer,
            ObjectProvider<PlatformTransactionManager> transactionManager, ObjectProvider<Clock> clock) {
        this.bookings = bookings;
        this.courtSlotRules = courtSlotRules.getIfAvailable(UnconfiguredCourtSlotRules::new);
        this.currentPlayer = currentPlayer.getIfAvailable(UnauthenticatedCurrentPlayer::new);
        this.transactions = new TransactionTemplate(
                transactionManager.getIfAvailable(() -> new DataSourceTransactionManager(dataSource)));
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Bean
    CreatePendingBooking createPendingBooking(BookingEventPublisher events) {
        return new CreatePendingBooking(bookings, courtSlotRules, currentPlayer, events, transactions, clock);
    }

    @Bean
    ConfirmBooking confirmBooking(BookingEventPublisher events) {
        return new ConfirmBooking(bookings, currentPlayer, events, transactions, clock);
    }

    @Bean
    CancelOwnPendingBooking cancelOwnPendingBooking(BookingEventPublisher events) {
        return new CancelOwnPendingBooking(bookings, currentPlayer, events, transactions, clock);
    }

    @Bean
    GetBooking getBooking() {
        return new GetBooking(bookings, currentPlayer, clock);
    }

    @Bean
    GetCourtOccupancy getCourtOccupancy() {
        return new GetCourtOccupancy(bookings, courtSlotRules, currentPlayer, clock);
    }
}
