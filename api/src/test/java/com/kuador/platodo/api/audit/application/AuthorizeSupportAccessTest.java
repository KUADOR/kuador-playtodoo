package com.kuador.platodo.api.audit.application;

import static com.kuador.platodo.api.audit.support.SupportCallers.as;
import static com.kuador.platodo.api.audit.support.SupportCallers.operator;
import static com.kuador.platodo.api.audit.support.SupportCallers.superAdmin;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.audit.domain.AccessReason;
import com.kuador.platodo.api.audit.domain.AccessSession;
import com.kuador.platodo.api.audit.domain.DenialCode;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.RecordingAuditLog;
import com.kuador.platodo.api.audit.support.SupportCallers;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantDirectory.TenantProfile;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization.SupportAccessAttempt;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * RecordAccess (D6, ADR-020): every request on the host of a complex is recorded before it runs, and only a valid
 * session of the caller on that complex, with a role that still allows its status, lets it through.
 */
class AuthorizeSupportAccessTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId B = new TenantId("0b0b0b0b-0000-4000-8000-00000000000b");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String ROUTE = "/api/v1/staff/bookings/{bookingId}";
    private static final String OPERATION = "getStaffBooking";
    private static final UUID SUPER_ADMIN_ID = UUID.randomUUID();
    private static final UUID OPERATOR_ID = UUID.randomUUID();

    private final MutableClock clock = new MutableClock(NOW);
    private final SupportCallers callers = new SupportCallers();
    private final Map<TenantId, TenantStatus> statuses = new ConcurrentHashMap<>();
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final AuthorizeSupportAccess authorize = new AuthorizeSupportAccess(new AuthorizeSupportAccess.Ports(
            callers.current(), this::lockedProfileOf, sessions, auditLog, TransactionOperations.withoutTransaction()),
            clock);

    @Test
    void aValidSessionOnItsComplexIsAllowedAndRecordedWithItsReasonAndOperation() {
        AccessSession session = sessionOf(SUPER_ADMIN_ID, A);

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, TenantStatus.ACTIVE, session.id().toString())).isTrue();

        assertThat(auditLog.events()).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo(AccessOutcome.ALLOWED);
            assertThat(event.sessionId()).contains(session.id());
            assertThat(event.platformUserId()).isEqualTo(SUPER_ADMIN_ID);
            assertThat(event.tenant()).isEqualTo(A);
            assertThat(event.reason()).contains("Ticket 1234");
            assertThat(event.request().httpMethod()).isEqualTo("GET");
            assertThat(event.request().routeTemplate()).isEqualTo(ROUTE);
            assertThat(event.request().operationId()).contains(OPERATION);
            assertThat(event.denialCode()).isEmpty();
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    /** Isolation: the session of A used on the host of B is refused and recorded on B without A's reason. */
    @Test
    void theSessionOfAUsedOnBIsDeniedAndRecordedOnBWithoutTheReasonOfA() {
        AccessSession sessionOnA = sessionOf(SUPER_ADMIN_ID, A);

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), B, TenantStatus.ACTIVE, sessionOnA.id().toString()))
                .isFalse();

        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, B, SUPER_ADMIN_ID));
    }

    @Test
    void aRequestWithoutSessionOrWithAMalformedOrUnknownOneIsDenied() {
        AuthenticatedPrincipal caller = superAdmin(SUPER_ADMIN_ID);

        assertThat(authorizeAs(caller, A, TenantStatus.ACTIVE, null)).isFalse();
        assertThat(authorizeAs(caller, A, TenantStatus.ACTIVE, "not-a-session")).isFalse();
        assertThat(authorizeAs(caller, A, TenantStatus.ACTIVE, UUID.randomUUID().toString())).isFalse();

        assertThat(auditLog.events()).hasSize(3).allSatisfy(event -> assertDenied(event, A, SUPER_ADMIN_ID));
    }

    @Test
    void theSessionOfAnotherPlatformUserIsDenied() {
        AccessSession othersSession = sessionOf(UUID.randomUUID(), A);

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, TenantStatus.ACTIVE, othersSession.id().toString()))
                .isFalse();
    }

    @Test
    void anExpiredOrClosedSessionIsDenied() {
        AccessSession expiring = sessionOf(SUPER_ADMIN_ID, A);
        AccessSession closed = sessionOf(SUPER_ADMIN_ID, A);
        sessions.close(closed.id(), NOW);

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, TenantStatus.ACTIVE, closed.id().toString())).isFalse();
        clock.setTo(NOW.plus(TTL));
        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, TenantStatus.ACTIVE, expiring.id().toString())).isFalse();
    }

    /** ADR-017: a capability the SUPER_ADMIN disables fails on the operator's next request, even with a session. */
    @Test
    void anOperatorIsDeniedFromTheRequestAfterTheCapabilityIsDisabled() {
        callers.tenantDataAccessForOperators(true);
        AccessSession session = sessionOf(OPERATOR_ID, A);
        assertThat(authorizeAs(operator(OPERATOR_ID), A, TenantStatus.ACTIVE, session.id().toString())).isTrue();

        callers.tenantDataAccessForOperators(false);

        assertThat(authorizeAs(operator(OPERATOR_ID), A, TenantStatus.ACTIVE, session.id().toString())).isFalse();
        assertThat(auditLog.events().getLast().denialCode()).contains(DenialCode.SUPPORT_SESSION_INVALID);
    }

    /** ADR-020: when the complex is suspended after the session was opened, the operator's session stops working. */
    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void anOperatorSessionStopsWorkingOnceTheComplexIsSuspended(TenantStatus status) {
        callers.tenantDataAccessForOperators(true);
        AccessSession session = sessionOf(OPERATOR_ID, A);

        assertThat(authorizeAs(operator(OPERATOR_ID), A, status, session.id().toString())).isFalse();
        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, status,
                sessionOf(SUPER_ADMIN_ID, A).id().toString())).isTrue();
    }

    @Test
    void aPlatformRoleThisCodeDoesNotKnowIsDeniedAndRecorded() {
        UUID legacyId = UUID.randomUUID();
        AccessSession session = sessionOf(legacyId, A);
        AuthenticatedPrincipal legacyOwner = AuthenticatedPrincipal.global(legacyId, AccountType.PLATFORM,
                "PLATFORM_OWNER");

        assertThat(authorizeAs(legacyOwner, A, TenantStatus.ACTIVE, session.id().toString())).isFalse();
        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, A, legacyId));
    }

    /** CA20 of PLATODO-26: when the audit cannot be written the request is denied. */
    @Test
    void whenTheAuditCannotBeWrittenTheRequestIsDenied() {
        AccessSession session = sessionOf(SUPER_ADMIN_ID, A);
        auditLog.failNextAppends();

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, TenantStatus.ACTIVE, session.id().toString())).isFalse();
    }

    /** A complex purged, or gone, while the request waited for its registry row is denied, not reached. */
    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"PURGING", "PURGED"})
    void aComplexWhosePurgeStartedIsDenied(TenantStatus status) {
        AccessSession session = sessionOf(SUPER_ADMIN_ID, A);

        assertThat(authorizeAs(superAdmin(SUPER_ADMIN_ID), A, status, session.id().toString())).isFalse();
        statuses.clear();
        assertThat(authorize.authorize(new SupportAccessAttempt(SUPER_ADMIN_ID, A,
                Optional.of(session.id().toString()), "GET", ROUTE, Optional.of(OPERATION)))).isFalse();
    }

    @Test
    void aRouteThatIsNotAContractOperationIsRecordedWithoutOperationId() {
        AccessSession session = sessionOf(SUPER_ADMIN_ID, A);
        statuses.put(A, TenantStatus.ACTIVE);

        as(superAdmin(SUPER_ADMIN_ID), () -> authorize.authorize(new SupportAccessAttempt(SUPER_ADMIN_ID, A,
                Optional.of(session.id().toString()), "GET", "[unmatched]", Optional.empty())));

        assertThat(auditLog.events().getFirst().request().operationId()).isEmpty();
    }

    private AccessSession sessionOf(UUID platformUserId, TenantId tenant) {
        AccessSession session = AccessSession.open(platformUserId, tenant, new AccessReason("Ticket 1234"), NOW, TTL);
        sessions.add(session);
        return session;
    }

    /** Authorizes a request of {@code caller} while the registry holds {@code tenant} in {@code status}. */
    private boolean authorizeAs(AuthenticatedPrincipal caller, TenantId tenant, TenantStatus status, String header) {
        statuses.put(tenant, status);
        SupportAccessAttempt attempt = new SupportAccessAttempt(caller.accountId(), tenant,
                Optional.ofNullable(header), "GET", ROUTE, Optional.of(OPERATION));
        return as(caller, () -> authorize.authorize(attempt));
    }

    private Optional<TenantProfile> lockedProfileOf(TenantId tenant) {
        return Optional.ofNullable(statuses.get(tenant)).map(status -> new TenantProfile(tenant, "Test complex",
                status, "test", null, NOW, null, null, null, null, null, 0));
    }

    private static void assertDenied(AccessAuditEvent event, TenantId tenant, UUID platformUserId) {
        assertThat(event.outcome()).isEqualTo(AccessOutcome.DENIED);
        assertThat(event.denialCode()).contains(DenialCode.SUPPORT_SESSION_INVALID);
        assertThat(event.tenant()).isEqualTo(tenant);
        assertThat(event.platformUserId()).isEqualTo(platformUserId);
        assertThat(event.sessionId()).isEmpty();
        assertThat(event.reason()).isEmpty();
    }
}
