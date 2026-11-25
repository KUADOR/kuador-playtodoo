package com.kuador.platodo.api.identity.infrastructure.persistence.tenant;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Row of {@code staff_user} in the schema of the current tenant (tenant migration V2), read-only here: provisioning
 * creates the owner and staff management arrives with its own epic.
 */
@Entity
@Table(name = "staff_user")
class StaffUserEntity {

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

    protected StaffUserEntity() {
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
}
