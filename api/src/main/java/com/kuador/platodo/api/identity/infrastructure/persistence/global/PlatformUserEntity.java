package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.platform_user} (migrations V3 and V6), read-only here. */
@Entity
@Table(name = "platform_user")
class PlatformUserEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String role;
    @Column(nullable = false)
    private String status;
    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected PlatformUserEntity() {
    }

    UUID id() {
        return id;
    }

    String passwordHash() {
        return passwordHash;
    }

    String role() {
        return role;
    }

    String displayName() {
        return displayName;
    }
}
