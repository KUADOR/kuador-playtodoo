package com.kuador.platodo.api.bookings.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import com.kuador.platodo.api.bookings.application.port.out.BookingEventPublisher;
import com.kuador.platodo.api.bookings.application.port.out.BookingRepository;
import com.kuador.platodo.api.bookings.infrastructure.events.SpringBookingEventPublisher;
import com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepository;
import com.kuador.platodo.api.bookings.infrastructure.web.ConditionalOnAnyBookingApiEnabled;

/**
 * Adapters shared by the player and staff booking use cases. Needs a {@link DataSource} whose connections use
 * the search_path of the resolved tenant (Tenancy).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAnyBookingApiEnabled
class BookingsConfiguration {

    /**
     * Lets consumers of booking events use {@code @TransactionalEventListener(AFTER_COMMIT)}. Without it,
     * nothing registers the factory in this application (there is no transaction auto-configuration).
     */
    @Bean
    static TransactionalEventListenerFactory bookingTransactionalEventListenerFactory() {
        return new TransactionalEventListenerFactory();
    }

    @Bean
    BookingRepository bookingRepository(DataSource dataSource) {
        return new PostgresBookingRepository(JdbcClient.create(dataSource));
    }

    @Bean
    BookingEventPublisher bookingEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringBookingEventPublisher(publisher);
    }
}
