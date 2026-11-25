package com.kuador.platodo.api.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.audit.application.OpenAccessSession.OpenedSession;
import com.kuador.platodo.api.audit.domain.ReasonRequiredException;
import com.kuador.platodo.api.audit.support.InMemoryAccessSessions;
import com.kuador.platodo.api.audit.support.StubPlatformOwner;
import com.kuador.platodo.api.audit.support.StubTenantDirectory;
import com.kuador.platodo.api.bookings.support.MutableClock;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.domain.TenantStatus;

class OpenAccessSessionTest {

    private static final TenantId A = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final TenantId PURGED = new TenantId("0c0c0c0c-0000-4000-8000-00000000000c");
    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String HOST_OF_A = "canchas-el-sol.playtodoo.com";
    private static final UUID OWNER = UUID.randomUUID();

    private final StubPlatformOwner owner = new StubPlatformOwner(OWNER);
    private final StubTenantDirectory tenants = new StubTenantDirectory();
    private final InMemoryAccessSessions sessions = new InMemoryAccessSessions();
    private final OpenAccessSession open = new OpenAccessSession(owner, tenants, sessions,
            TransactionOperations.withoutTransaction(), TTL, new MutableClock(NOW));

    OpenAccessSessionTest() {
        tenants.add(A, TenantStatus.ACTIVE, HOST_OF_A);
        tenants.add(PURGED, TenantStatus.PURGED, null);
    }

    @Test
    void theOwnerOpensASessionBoundToTheComplexWithItsReason() {
        OpenedSession opened = open.open(A, "  Ticket 1234 ");

        assertThat(opened.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(opened.tenantHost()).isEqualTo(HOST_OF_A);
        assertThat(sessions.find(opened.sessionId())).hasValueSatisfying(session -> {
            assertThat(session.platformUserId()).isEqualTo(OWNER);
            assertThat(session.tenant()).isEqualTo(A);
            assertThat(session.reason().value()).isEqualTo("Ticket 1234");
            assertThat(session.isOpenAt(NOW)).isTrue();
        });
    }

    /** Criterion 10. */
    @Test
    void aCallerThatIsNotTheOwnerCannotOpenASession() {
        owner.actAsNonOwner();

        assertThatThrownBy(() -> open.open(A, "Ticket 1234")).isInstanceOf(NotPlatformOwnerException.class);
    }

    /** Criterion 11: no reason, no session. */
    @Test
    void aSessionWithoutReasonIsRefused() {
        assertThatThrownBy(() -> open.open(A, " ")).isInstanceOf(ReasonRequiredException.class);
        assertThatThrownBy(() -> open.open(A, null)).isInstanceOf(ReasonRequiredException.class);
    }

    @Test
    void anUnknownOrPurgedComplexIsNotFound() {
        assertThatThrownBy(() -> open.open(new TenantId(UUID.randomUUID().toString()), "Ticket 1234"))
                .isInstanceOf(AccessTenantNotFoundException.class);
        assertThatThrownBy(() -> open.open(PURGED, "Ticket 1234"))
                .isInstanceOf(AccessTenantNotFoundException.class);
    }

    @Test
    void theLifetimeMustBePositive() {
        assertThatThrownBy(() -> new OpenAccessSession(owner, tenants, sessions,
                TransactionOperations.withoutTransaction(), Duration.ZERO, new MutableClock(NOW)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
