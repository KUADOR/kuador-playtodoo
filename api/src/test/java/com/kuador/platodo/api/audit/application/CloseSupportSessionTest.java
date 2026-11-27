package com.kuador.platodo.api.audit.application;

import static com.kuador.platodo.api.audit.support.SupportCallers.as;
import static com.kuador.platodo.api.audit.support.SupportCallers.operator;
import static com.kuador.platodo.api.audit.support.SupportCallers.superAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.RecordingAuditLog;
import com.kuador.platodo.api.audit.support.SupportCallers;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.event.TenantPurged;

/** Closing support sessions: by whoever opened them, and all of them when the complex is purged (ADR-020). */
class CloseSupportSessionTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final UUID OPERATOR_ID = UUID.randomUUID();
    private static final AuditedRequest CLOSING = new AuditedRequest("DELETE",
            "/api/v1/platform/support-sessions/{sessionId}", Optional.of("closeSupportSession"));

    private final MutableClock clock = new MutableClock(NOW);
    private final SupportCallers callers = new SupportCallers();
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final CloseSupportSession closeSession = new CloseSupportSession(new CloseSupportSession.Ports(
            callers.guard(), sessions, auditLog, TransactionOperations.withoutTransaction()), clock);

    @Test
    void whoOpenedASessionClosesItAuditedAndClosingAgainChangesNothing() {
        AccessSession session = sessionOf(OPERATOR_ID, A);

        as(operator(OPERATOR_ID), () -> {
            closeSession.close(session.id(), CLOSING);
            return null;
        });
        clock.setTo(NOW.plusSeconds(1));
        as(operator(OPERATOR_ID), () -> {
            closeSession.close(session.id(), CLOSING);
            return null;
        });

        assertThat(sessions.find(session.id())).hasValueSatisfying(
                closed -> assertThat(closed.closedAt()).contains(NOW));
        assertThat(auditLog.events()).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo(AccessOutcome.ALLOWED);
            assertThat(event.sessionId()).contains(session.id());
            assertThat(event.request()).isEqualTo(CLOSING);
        });
    }

    @Test
    void anotherPlatformUserCannotCloseItNorAnUnknownOne() {
        AccessSession session = sessionOf(OPERATOR_ID, A);

        assertThatThrownBy(() -> as(superAdmin(UUID.randomUUID()), () -> {
            closeSession.close(session.id(), CLOSING);
            return null;
        })).isInstanceOf(SupportSessionNotFoundException.class);
        assertThatThrownBy(() -> as(operator(OPERATOR_ID), () -> {
            closeSession.close(UUID.randomUUID(), CLOSING);
            return null;
        })).isInstanceOf(SupportSessionNotFoundException.class);
        assertThat(sessions.find(session.id()).orElseThrow().isOpenAt(NOW)).isTrue();
        assertThat(auditLog.events()).isEmpty();
    }

    @Test
    void thePurgeOfAComplexClosesItsOpenSessionsOnly() {
        AccessSession onA = sessionOf(OPERATOR_ID, A);
        AccessSession onB = sessionOf(OPERATOR_ID, B);
        Instant purgedAt = NOW.plusSeconds(60);

        int closed = new CloseSupportSessionsOfPurgedTenant(sessions).close(new TenantPurged(A, purgedAt));

        assertThat(closed).isOne();
        assertThat(sessions.find(onA.id()).orElseThrow().isOpenAt(purgedAt)).isFalse();
        assertThat(sessions.find(onB.id()).orElseThrow().isOpenAt(purgedAt)).isTrue();
    }

    private AccessSession sessionOf(UUID platformUserId, TenantId tenant) {
        AccessSession session = AccessSession.open(platformUserId, tenant, new AccessReason("Ticket 1234"), NOW, TTL);
        sessions.add(session);
        return session;
    }
}
