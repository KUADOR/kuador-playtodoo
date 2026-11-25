package com.kuador.platodo.api.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.RecordingAuditLog;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.PlatformAccessAuthorization.PlatformAccessAttempt;

/** RecordAccess (D6): every attempt is recorded before it runs, and only a valid session of the owner allows it. */
class AuthorizePlatformAccessTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String ROUTE = "/api/v1/staff/bookings/{bookingId}";
    private static final UUID OWNER = UUID.randomUUID();

    private final MutableClock clock = new MutableClock(NOW);
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final AuthorizePlatformAccess authorize =
            new AuthorizePlatformAccess(sessions, auditLog, TransactionOperations.withoutTransaction(), clock);

    private final AccessSession sessionOnA = AccessSession.open(OWNER, A, new AccessReason("Ticket 1234"), NOW, TTL);

    @Test
    void aValidSessionOfTheOwnerOnItsComplexIsAllowedAndRecordedWithItsReason() {
        sessions.add(sessionOnA);

        assertThat(authorize.authorize(attempt(OWNER, true, A, sessionOnA.id().toString()))).isTrue();

        assertThat(auditLog.events()).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo(AccessOutcome.ALLOWED);
            assertThat(event.sessionId()).contains(sessionOnA.id());
            assertThat(event.platformUserId()).isEqualTo(OWNER);
            assertThat(event.tenant()).isEqualTo(A);
            assertThat(event.reason()).contains("Ticket 1234");
            assertThat(event.httpMethod()).isEqualTo("GET");
            assertThat(event.routeTemplate()).isEqualTo(ROUTE);
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void theSessionOfAUsedOnBIsDeniedAndRecordedOnBWithoutTheReasonOfA() {
        sessions.add(sessionOnA);

        assertThat(authorize.authorize(attempt(OWNER, true, B, sessionOnA.id().toString()))).isFalse();

        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, B));
    }

    @Test
    void aRequestWithoutSessionIsDeniedAndRecorded() {
        assertThat(authorize.authorize(attempt(OWNER, true, A, null))).isFalse();

        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, A));
    }

    @Test
    void aMalformedOrUnknownSessionIsDenied() {
        assertThat(authorize.authorize(attempt(OWNER, true, A, "not-a-session"))).isFalse();
        assertThat(authorize.authorize(attempt(OWNER, true, A, UUID.randomUUID().toString()))).isFalse();

        assertThat(auditLog.events()).hasSize(2).allSatisfy(event -> assertDenied(event, A));
    }

    @Test
    void anExpiredSessionIsDenied() {
        sessions.add(sessionOnA);
        clock.advance(TTL);

        assertThat(authorize.authorize(attempt(OWNER, true, A, sessionOnA.id().toString()))).isFalse();
    }

    @Test
    void aClosedSessionIsDenied() {
        sessions.add(sessionOnA);
        sessions.close(sessionOnA.id(), NOW);
        clock.advance(Duration.ofSeconds(1));

        assertThat(authorize.authorize(attempt(OWNER, true, A, sessionOnA.id().toString()))).isFalse();
    }

    @Test
    void theSessionOfAnotherPlatformUserIsDenied() {
        sessions.add(sessionOnA);
        UUID otherUser = UUID.randomUUID();

        assertThat(authorize.authorize(attempt(otherUser, true, A, sessionOnA.id().toString()))).isFalse();
        assertThat(auditLog.events()).singleElement()
                .satisfies(event -> assertThat(event.platformUserId()).isEqualTo(otherUser));
    }

    /** Criterion 10: a Kuador user that is not the owner never reaches the data, even with a valid session id. */
    @Test
    void aPlatformUserThatIsNotTheOwnerIsDeniedEvenWithAValidSession() {
        sessions.add(sessionOnA);

        assertThat(authorize.authorize(attempt(OWNER, false, A, sessionOnA.id().toString()))).isFalse();
        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, A));
    }

    /** Criterion 20: when the audit cannot be written, a valid session is denied. */
    @Test
    void whenTheAuditCannotBeWrittenTheAccessIsDenied() {
        sessions.add(sessionOnA);
        auditLog.failNextAppends();

        assertThat(authorize.authorize(attempt(OWNER, true, A, sessionOnA.id().toString()))).isFalse();
        assertThat(auditLog.events()).isEmpty();
    }

    private static void assertDenied(AccessAuditEvent event, TenantId tenant) {
        assertThat(event.outcome()).isEqualTo(AccessOutcome.DENIED);
        assertThat(event.tenant()).isEqualTo(tenant);
        assertThat(event.sessionId()).isEmpty();
        assertThat(event.reason()).isEmpty();
    }

    private static PlatformAccessAttempt attempt(UUID user, boolean owner, TenantId tenant, String header) {
        return new PlatformAccessAttempt(user, owner, tenant, Optional.ofNullable(header), "GET", ROUTE);
    }
}
