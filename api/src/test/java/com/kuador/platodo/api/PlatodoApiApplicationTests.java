package com.kuador.platodo.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.kuador.platodo.api.bookings.application.CreatePendingBooking;

@SpringBootTest
class PlatodoApiApplicationTests {

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
