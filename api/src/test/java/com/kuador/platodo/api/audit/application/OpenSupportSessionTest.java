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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.OpenSupportSession.OpenedSession;
import com.kuador.platodo.api.audit.domain.AccessAuditEvent;
import com.kuador.platodo.api.audit.domain.AccessOutcome;
import com.kuador.platodo.api.audit.domain.AuditedRequest;
import com.kuador.platodo.api.audit.domain.DenialCode;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.RecordingAuditLog;
import com.kuador.platodo.api.audit.support.StubTenantDirectory;
import com.kuador.platodo.api.audit.support.SupportCallers;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerForbiddenException;
import com.kuador.platodo.api.identity.application.port.in.PlatformCallerNotAuthenticatedException;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/** Opening a support session (ADR-020; PLATODO-27 CA11 to CA13): who, on which status, and only with a reason. */
class OpenSupportSessionTest {

    private static final TenantId COMPLEX = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final String HOST = "canchas-el-sol.localhost";
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String REASON = "Complaint about a duplicated booking, ticket 1234";
    private static final AuditedRequest OPENING = new AuditedRequest("POST",
            "/api/v1/platform/complexes/{complexId}/support-sessions", Optional.of("openSupportSession"));
    private static final UUID SUPER_ADMIN_ID = UUID.randomUUID();
    private static final UUID OPERATOR_ID = UUID.randomUUID();

    private final SupportCallers callers = new SupportCallers();
    private final StubTenantDirectory tenants = new StubTenantDirectory();
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final OpenSupportSession openSession = new OpenSupportSession(new OpenSupportSession.Ports(
            callers.current(), tenants, sessions, auditLog, TransactionOperations.withoutTransaction()),
            TTL, new MutableClock(NOW));

    @Test
    void theSuperAdminOpensASessionOnAnActiveComplexAndTheOpeningIsAudited() {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);

        OpenedSession opened = as(superAdmin(SUPER_ADMIN_ID), () -> openSession.open(COMPLEX, REASON, OPENING));

        assertThat(opened.tenantHost()).isEqualTo(HOST);
        assertThat(opened.tenantStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(opened.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(sessions.find(opened.sessionId())).hasValueSatisfying(session -> {
            assertThat(session.platformUserId()).isEqualTo(SUPER_ADMIN_ID);
            assertThat(session.tenant()).isEqualTo(COMPLEX);
            assertThat(session.reason().value()).isEqualTo(REASON);
        });
        assertThat(auditLog.events()).singleElement().satisfies(event -> {
            assertThat(event.outcome()).isEqualTo(AccessOutcome.ALLOWED);
            assertThat(event.sessionId()).contains(opened.sessionId());
            assertThat(event.request()).isEqualTo(OPENING);
            assertThat(event.reason()).contains(REASON);
            assertThat(event.denialCode()).isEmpty();
        });
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void theSuperAdminAlsoOpensASessionOnASuspendedOrArchivedComplex(TenantStatus status) {
        tenants.add(COMPLEX, status, HOST);

        OpenedSession opened = as(superAdmin(SUPER_ADMIN_ID), () -> openSession.open(COMPLEX, REASON, OPENING));

        assertThat(opened.tenantStatus()).isEqualTo(status);
    }

    @Test
    void anOperatorWithTheCapabilityOpensASessionOnAnActiveComplex() {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);
        callers.tenantDataAccessForOperators(true);

        OpenedSession opened = as(operator(OPERATOR_ID), () -> openSession.open(COMPLEX, REASON, OPENING));

        assertThat(sessions.find(opened.sessionId())).hasValueSatisfying(
                session -> assertThat(session.platformUserId()).isEqualTo(OPERATOR_ID));
    }

    @Test
    void anOperatorWithoutTheCapabilityIsRefusedAndTheRefusalIsAudited() {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);

        assertRefused(operator(OPERATOR_ID), REASON, DenialCode.CAPABILITY_REVOKED);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"SUSPENDED", "ARCHIVED"})
    void anOperatorIsRefusedOnASuspendedOrArchivedComplex(TenantStatus status) {
        tenants.add(COMPLEX, status, HOST);
        callers.tenantDataAccessForOperators(true);

        assertRefused(operator(OPERATOR_ID), REASON, DenialCode.TENANT_NOT_ACCESSIBLE);
    }

    @ParameterizedTest
    @EnumSource(value = TenantStatus.class, names = {"PROVISIONING", "PROVISIONING_FAILED", "PURGING", "PURGED"})
    void nobodyOpensASessionOnAComplexThatIsNotOperating(TenantStatus status) {
        tenants.add(COMPLEX, status, null);

        assertRefused(superAdmin(SUPER_ADMIN_ID), REASON, DenialCode.TENANT_NOT_ACCESSIBLE);
    }

    /** CA11: without a reason no session is opened, so there is no access to any data. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void withoutAReasonNoSessionIsOpened(String reason) {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);

        assertRefused(superAdmin(SUPER_ADMIN_ID), reason, DenialCode.REASON_REQUIRED);
    }

    @Test
    void aReasonLongerThanTheLimitIsRequiredToo() {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);

        assertRefused(superAdmin(SUPER_ADMIN_ID), "x".repeat(501), DenialCode.REASON_REQUIRED);
    }

    @Test
    void anUnknownComplexIsNotFoundAndNothingIsRecorded() {
        assertThatThrownBy(() -> as(superAdmin(SUPER_ADMIN_ID), () -> openSession.open(COMPLEX, REASON, OPENING)))
                .isInstanceOf(SupportComplexNotFoundException.class);

        assertThat(auditLog.events()).isEmpty();
    }

    @Test
    void anonymousCallersStaffAndPlayersCannotOpenASession() {
        tenants.add(COMPLEX, TenantStatus.ACTIVE, HOST);
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLAYER, "PLAYER");
        AuthenticatedPrincipal staff = AuthenticatedPrincipal.staff(UUID.randomUUID(), COMPLEX, "OWNER");

        assertThatThrownBy(() -> openSession.open(COMPLEX, REASON, OPENING))
                .isInstanceOf(PlatformCallerNotAuthenticatedException.class);
        assertThatThrownBy(() -> as(player, () -> openSession.open(COMPLEX, REASON, OPENING)))
                .isInstanceOf(PlatformCallerForbiddenException.class);
        assertThatThrownBy(() -> as(staff, () -> openSession.open(COMPLEX, REASON, OPENING)))
                .isInstanceOf(PlatformCallerForbiddenException.class);
        assertThat(auditLog.events()).isEmpty();
    }

    private void assertRefused(AuthenticatedPrincipal caller, String reason, DenialCode expected) {
        assertThatThrownBy(() -> as(caller, () -> openSession.open(COMPLEX, reason, OPENING)))
                .isInstanceOfSatisfying(SupportSessionRefusedException.class,
                        refusal -> assertThat(refusal.code()).isEqualTo(expected));

        assertThat(auditLog.events()).singleElement().satisfies(event -> assertDenied(event, caller, expected));
        assertThat(sessions.closeOpenOf(COMPLEX, NOW)).isZero();
    }

    private static void assertDenied(AccessAuditEvent event, AuthenticatedPrincipal caller, DenialCode expected) {
        assertThat(event.outcome()).isEqualTo(AccessOutcome.DENIED);
        assertThat(event.denialCode()).contains(expected);
        assertThat(event.platformUserId()).isEqualTo(caller.accountId());
        assertThat(event.tenant()).isEqualTo(COMPLEX);
        assertThat(event.sessionId()).isEmpty();
        assertThat(event.reason()).isEmpty();
        assertThat(event.request()).isEqualTo(OPENING);
    }
}
