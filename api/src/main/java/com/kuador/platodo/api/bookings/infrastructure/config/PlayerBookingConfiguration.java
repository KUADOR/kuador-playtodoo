package com.kuador.platodo.api.bookings.infrastructure.config;

import java.time.Clock;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
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
import com.kuador.platodo.api.bookings.infrastructure.events.SpringBookingEventPublisher;
import com.kuador.platodo.api.bookings.infrastructure.failclosed.UnauthenticatedCurrentPlayer;
import com.kuador.platodo.api.bookings.infrastructure.failclosed.UnconfiguredCourtSlotRules;
import com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepository;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnPlayerApiEnabled;

/**
 * Wires the player booking use cases. Needs a {@link DataSource} whose connections use the search_path of the
 * resolved tenant (Tenancy). Until the Courts and Identity modules register their {@link CourtSlotRules} and
 * {@link CurrentPlayer} beans, fail-closed defaults reject every request.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnPlayerApiEnabled
class PlayerBookingConfiguration {

    private final BookingRepository bookings;
    private final CourtSlotRules courtSlotRules;
    private final CurrentPlayer currentPlayer;
    private final TransactionOperations transactions;
    private final Clock clock;

    PlayerBookingConfiguration(DataSource dataSource, ObjectProvider<CourtSlotRules> courtSlotRules,
            ObjectProvider<CurrentPlayer> currentPlayer, ObjectProvider<PlatformTransactionManager> transactionManager,
            ObjectProvider<Clock> clock) {
        this.bookings = new PostgresBookingRepository(JdbcClient.create(dataSource));
        this.courtSlotRules = courtSlotRules.getIfAvailable(UnconfiguredCourtSlotRules::new);
        this.currentPlayer = currentPlayer.getIfAvailable(UnauthenticatedCurrentPlayer::new);
        this.transactions = new TransactionTemplate(
                transactionManager.getIfAvailable(() -> new DataSourceTransactionManager(dataSource)));
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    /**
     * Lets consumers of booking events use {@code @TransactionalEventListener(AFTER_COMMIT)}. Without it,
     * nothing registers the factory in this application (there is no transaction auto-configuration).
     */
    @Bean
    static TransactionalEventListenerFactory bookingTransactionalEventListenerFactory() {
        return new TransactionalEventListenerFactory();
    }

    @Bean
    BookingEventPublisher bookingEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringBookingEventPublisher(publisher);
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
