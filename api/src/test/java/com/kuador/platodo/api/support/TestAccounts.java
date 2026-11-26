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
    /** Visible name of every platform user created here. */
    public static final String PLATFORM_USER_NAME = "Test platform user";

    private static final String ACTIVE = "ACTIVE";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String OPERATOR = "OPERATOR";

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

    /** An active Kuador owner (role {@code SUPER_ADMIN}, formerly {@code PLATFORM_OWNER}) with {@link #PASSWORD}. */
    public static UUID platformOwner(String email) {
        return platformUser(email, SUPER_ADMIN);
    }

    /** An active Kuador operator (role {@code OPERATOR}) with {@link #PASSWORD}. */
    public static UUID operator(String email) {
        return platformUser(email, OPERATOR);
    }

    private static UUID platformUser(String email, String role) {
        UUID id = UUID.randomUUID();
        TenantDatabase.admin().sql("""
                        INSERT INTO global.platform_user (id, email, password_hash, role, status, display_name,
                                                          created_at)
                        VALUES (:id, :email, :hash, :role, :status, :name, now())
                        """)
                .param("id", id)
                .param("email", email)
                .param("hash", PASSWORDS.encode(PASSWORD))
                .param("role", role)
                .param("status", ACTIVE)
                .param("name", PLATFORM_USER_NAME)
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
