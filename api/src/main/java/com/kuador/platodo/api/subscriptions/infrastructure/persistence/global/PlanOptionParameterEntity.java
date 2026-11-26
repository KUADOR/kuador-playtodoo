package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Row of {@code global.platform_parameter} (migration V7), read-only here: Plans and subscriptions only reads the
 * {@code plan.*} parameters. {@code value} is the jsonb value as text.
 */
@Entity
@Immutable
@Table(name = "platform_parameter")
class PlanOptionParameterEntity {

    @Id
    private String key;
    @Column(nullable = false)
    private String value;

    protected PlanOptionParameterEntity() {
    }

    String value() {
        return value;
    }
}
