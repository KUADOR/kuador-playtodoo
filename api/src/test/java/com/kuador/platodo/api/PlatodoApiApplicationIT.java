package com.kuador.platodo.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.bookings.application.CreatePendingBooking;
import com.kuador.platodo.api.support.TenantDatabase;

/** The application starts against PostgreSQL: it now needs the database for its persistence units. */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class PlatodoApiApplicationIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void thePlayerBookingApiIsOffByDefault() {
        assertThat(context.getBeanNamesForType(CreatePendingBooking.class)).isEmpty();
    }
}
