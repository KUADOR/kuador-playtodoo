package com.kuador.platodo.api.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

class AccessSessionTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final UUID OWNER = UUID.randomUUID();

    private final AccessSession session = AccessSession.open(OWNER, A, new AccessReason("Ticket 1234"), NOW, TTL);

    @Test
    void grantsItsOwnerOnItsComplexWhileOpen() {
        assertThat(session.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(session.grants(OWNER, A, NOW)).isTrue();
        assertThat(session.grants(OWNER, A, NOW.plus(TTL).minusMillis(1))).isTrue();
    }

    @Test
    void grantsNothingOnAnotherComplex() {
        assertThat(session.grants(OWNER, B, NOW)).isFalse();
    }

    @Test
    void grantsNothingToAnotherPlatformUser() {
        assertThat(session.grants(UUID.randomUUID(), A, NOW)).isFalse();
    }

    @Test
    void grantsNothingOnceExpired() {
        assertThat(session.grants(OWNER, A, NOW.plus(TTL))).isFalse();
    }

    @Test
    void grantsNothingOnceClosed() {
        AccessSession closed = new AccessSession(session.id(), OWNER, A, session.reason(), session.startedAt(),
                session.expiresAt(), Optional.of(NOW.plusSeconds(1)));

        assertThat(closed.isOpenAt(NOW.plusSeconds(2))).isFalse();
        assertThat(closed.grants(OWNER, A, NOW.plusSeconds(2))).isFalse();
    }
}
