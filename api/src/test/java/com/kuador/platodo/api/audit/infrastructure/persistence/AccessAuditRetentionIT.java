package com.kuador.platodo.api.audit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.kuador.platodo.api.audit.application.DeleteExpiredAccessAudit;
import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestAccounts;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Retention of the access audit (ADR-016; PLATODO-27 CA13): one year, then the scheduled job deletes it. Only the test
 * database is touched; it is shared with other test classes, so assertions are about the rows this class inserts.
 */
@SpringBootTest
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class AccessAuditRetentionIT {

    private static final String EXPIRED = "366 days";
    private static final String KEPT = "364 days";

    @Autowired
    private DeleteExpiredAccessAudit deleteExpired;

    private TenantId tenant;
    private UUID kuadorUser;

    @BeforeEach
    void setUp() {
        tenant = new TenantId(UUID.randomUUID().toString());
        TestTenants.register(tenant, "ACTIVE");
        kuadorUser = TestAccounts.platformOwner(TestAccounts.uniqueEmail("kuador"));
    }

    @Test
    void theAuditOlderThanOneYearIsDeletedAndTheRestIsKept() {
        UUID expiredSession = session(EXPIRED);
        UUID expiredEvent = event(expiredSession, EXPIRED);
        UUID expiredDenial = event(null, EXPIRED);
        UUID keptSession = session(KEPT);
        UUID keptEvent = event(keptSession, KEPT);

        deleteExpired.run();

        assertThat(eventIds()).containsExactly(keptEvent).doesNotContain(expiredEvent, expiredDenial);
        assertThat(sessionIds()).containsExactly(keptSession);
    }

    /** A session past the cutoff stays while a newer event still refers to it, so the delete never breaks the audit. */
    @Test
    void aSessionWithAKeptEventIsKept() {
        UUID oldSession = session(EXPIRED);
        UUID keptEvent = event(oldSession, KEPT);

        deleteExpired.run();
        deleteExpired.run();

        assertThat(eventIds()).containsExactly(keptEvent);
        assertThat(sessionIds()).containsExactly(oldSession);
    }

    private UUID session(String age) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.access_session (id, platform_user_id, tenant_id, reason, started_at,
                            expires_at)
                        VALUES (:id, :user, :tenant::uuid, 'Soporte', now() - CAST(:age AS interval),
                                now() - CAST(:age AS interval) + interval '30 minutes')
                        """)
                .param("id", id).param("user", kuadorUser).param("tenant", tenant.value()).param("age", age)
                .update();
        return id;
    }

    /** An allowed request under {@code session}, or a denied one without session when it is null. */
    private UUID event(UUID session, String age) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.access_audit_event (id, session_id, platform_user_id, tenant_id, reason,
                            http_method, route_template, outcome, occurred_at)
                        VALUES (:id, :session, :user, :tenant::uuid, CASE WHEN :allowed THEN 'Soporte' END, 'GET',
                                '/api/v1/staff/bookings', CASE WHEN :allowed THEN 'ALLOWED' ELSE 'DENIED' END,
                                now() - CAST(:age AS interval))
                        """)
                .param("id", id).param("session", session).param("user", kuadorUser)
                .param("tenant", tenant.value()).param("allowed", session != null).param("age", age)
                .update();
        return id;
    }

    private List<UUID> eventIds() {
        return TenantDatabase.admin().sql("SELECT id FROM global.access_audit_event WHERE tenant_id = :id::uuid")
                .param("id", tenant.value()).query(UUID.class).list();
    }

    private List<UUID> sessionIds() {
        return TenantDatabase.admin().sql("SELECT id FROM global.access_session WHERE tenant_id = :id::uuid")
                .param("id", tenant.value()).query(UUID.class).list();
    }
}
