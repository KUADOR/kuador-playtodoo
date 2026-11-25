package com.kuador.platodo.api.support;

import java.util.UUID;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Accounts written straight into the database, until registration, invitations and staff management exist. Passwords
 * are stored hashed, as the api expects them. Needs a started application context, which creates the schemas.
 */
public final class TestAccounts {

    public static final String PASSWORD = "test-password";

    private static final PasswordEncoder PASSWORDS = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private static final String ACTIVE = "ACTIVE";

    private TestAccounts() {
    }

    /** An active staff member with {@link #PASSWORD} in the schema of {@code tenant}. */
    public static UUID staff(TenantId tenant, String email, String role) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO "%s".staff_user (id, email, password_hash, role, status, created_at)
                        VALUES (:id, :email, :hash, :role, :status, now())
                        """.formatted(TestTenants.schemaOf(tenant)))
                .param("id", id)
                .param("email", email)
                .param("hash", PASSWORDS.encode(PASSWORD))
                .param("role", role)
                .param("status", ACTIVE)
                .update();
        return id;
    }

    /** An active global player account with {@link #PASSWORD}. */
    public static UUID player(String email) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.player_account (id, email, password_hash, display_name, status, created_at)
                        VALUES (:id, :email, :hash, :name, :status, now())
                        """)
                .param("id", id)
                .param("email", email)
                .param("hash", PASSWORDS.encode(PASSWORD))
                .param("name", "Test player")
                .param("status", ACTIVE)
                .update();
        return id;
    }

    /** An active Kuador platform owner with {@link #PASSWORD}. */
    public static UUID platformOwner(String email) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.platform_user (id, email, password_hash, role, status, created_at)
                        VALUES (:id, :email, :hash, 'PLATFORM_OWNER', :status, now())
                        """)
                .param("id", id)
                .param("email", email)
                .param("hash", PASSWORDS.encode(PASSWORD))
                .param("status", ACTIVE)
                .update();
        return id;
    }

    /** Disables a global player account. */
    public static void disablePlayer(UUID playerId) {
        TenantDatabase.admin().sql("UPDATE global.player_account SET status = 'DISABLED' WHERE id = :id")
                .param("id", playerId)
                .update();
    }

    /** A unique e-mail for one test, so test classes sharing the database never collide. */
    public static String uniqueEmail(String name) {
        return name + "-" + UUID.randomUUID() + "@example.com";
    }
}
