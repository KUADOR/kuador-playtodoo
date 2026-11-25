package com.kuador.platodo.api.identity.infrastructure.persistence.global;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.player_account} (migration V3), read-only here: registration arrives with its own epic. */
@Entity
@Table(name = "player_account")
class PlayerAccountEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String status;

    protected PlayerAccountEntity() {
    }

    UUID id() {
        return id;
    }

    String passwordHash() {
        return passwordHash;
    }
}
