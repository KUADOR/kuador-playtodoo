package com.kuador.platodo.api.bookings.infrastructure.scheduling;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.kuador.platodo.api.bookings.application.port.in.ExpireDuePendingBookings;
import com.kuador.platodo.api.tenancy.application.port.in.ActiveTenantRunner;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Every {@link #INTERVAL_SECONDS} seconds, expires due pending bookings in each active tenant, one tenant
 * at a time through Tenancy. Safe to run on several api instances at once: see {@code ExpirePendingBookings}.
 */
public class ExpirePendingBookingsJob {

    /** Delay between runs. A late expiry never double-books: lazy expiry frees the slot on the next booking. */
    static final long INTERVAL_SECONDS = 30;

    private static final Logger log = LoggerFactory.getLogger(ExpirePendingBookingsJob.class);

    private final ActiveTenantRunner activeTenants;
    private final ExpireDuePendingBookings expireDuePendingBookings;

    public ExpirePendingBookingsJob(
            ActiveTenantRunner activeTenants, ExpireDuePendingBookings expireDuePendingBookings) {
        this.activeTenants = activeTenants;
        this.expireDuePendingBookings = expireDuePendingBookings;
    }

    @Scheduled(fixedDelay = INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void run() {
        activeTenants.forEachActiveTenant(this::expireIn);
    }

    /** A failure in one tenant is logged and retried on the next run; it must not stop the other tenants. */
    private void expireIn(TenantId tenant) {
        try {
            int expired = expireDuePendingBookings.expireDue(tenant.value());
            log.debug("Expired {} pending bookings in tenant {}", expired, tenant.value());
        } catch (RuntimeException error) {
            log.error("Could not expire pending bookings in tenant {}; retrying on the next run",
                    tenant.value(), error);
        }
    }
}
