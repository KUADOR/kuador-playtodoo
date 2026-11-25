package com.kuador.platodo.api.identity.infrastructure.persistence.tenant;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.kuador.platodo.api.identity.application.port.out.PasswordHashes;
import com.kuador.platodo.api.identity.domain.EmailAddress;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;

/**
 * The owner staff account Tenancy provisions in the schema of a new complex. The account is active but its password
 * is a random secret nobody knows, so nobody can log in with it until the owner sets one (invitation epic).
 */
public final class JdbcTenantOwnerAccounts implements TenantOwnerAccounts {

    private static final String OWNER = "OWNER";
    private static final String ACTIVE = "ACTIVE";
    private static final int SECRET_BYTES = 32;

    private final JdbcClient tenantData;
    private final PasswordHashes passwords;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /** {@code tenantData} must use the tenant data source: it fails closed without a {@code TenantScope}. */
    public JdbcTenantOwnerAccounts(JdbcClient tenantData, PasswordHashes passwords, Clock clock) {
        this.tenantData = tenantData;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Override
    public void createOwner(String email) {
        tenantData.sql("""
                        INSERT INTO staff_user (id, email, password_hash, role, status, created_at)
                        VALUES (:id, :email, :passwordHash, :role, :status, :createdAt)
                        ON CONFLICT (email) DO NOTHING
                        """)
                .param("id", UUID.randomUUID())
                .param("email", EmailAddress.normalize(email).value())
                .param("passwordHash", passwords.hash(unknownSecret()))
                .param("role", OWNER)
                .param("status", ACTIVE)
                .param("createdAt", clock.instant().atOffset(ZoneOffset.UTC))
                .update();
    }

    @Override
    public boolean hasOwner() {
        return tenantData.sql("SELECT EXISTS (SELECT 1 FROM staff_user WHERE role = :role)")
                .param("role", OWNER)
                .query(Boolean.class)
                .single();
    }

    private String unknownSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
