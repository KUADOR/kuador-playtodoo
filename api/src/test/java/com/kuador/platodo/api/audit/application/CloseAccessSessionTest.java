package com.kuador.platodo.api.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.StubPlatformOwner;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

class CloseAccessSessionTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final UUID OWNER = UUID.randomUUID();

    private final MutableClock clock = new MutableClock(NOW);
    private final StubPlatformOwner owner = new StubPlatformOwner(OWNER);
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final CloseAccessSession close =
            new CloseAccessSession(owner, sessions, TransactionOperations.withoutTransaction(), clock);
    private final AccessSession session = AccessSession.open(OWNER, A, new AccessReason("Ticket 1234"), NOW, TTL);

    CloseAccessSessionTest() {
        sessions.add(session);
    }

    @Test
    void theOwnerClosesTheirSessionAndClosingAgainChangesNothing() {
        clock.advance(Duration.ofMinutes(5));
        close.close(session.id());
        clock.advance(Duration.ofMinutes(5));
        close.close(session.id());

        assertThat(sessions.find(session.id()).orElseThrow().closedAt()).contains(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void closingAnExpiredSessionChangesNothing() {
        clock.advance(TTL);

        close.close(session.id());

        assertThat(sessions.find(session.id()).orElseThrow().closedAt()).isEmpty();
    }

    @Test
    void theSessionOfAnotherUserOrAnUnknownOneIsNotFound() {
        owner.actAs(UUID.randomUUID());

        assertThatThrownBy(() -> close.close(session.id())).isInstanceOf(AccessSessionNotFoundException.class);
        assertThatThrownBy(() -> close.close(UUID.randomUUID())).isInstanceOf(AccessSessionNotFoundException.class);
    }
}
