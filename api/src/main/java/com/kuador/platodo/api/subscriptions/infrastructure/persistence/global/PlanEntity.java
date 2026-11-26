package com.kuador.platodo.api.subscriptions.infrastructure.persistence.global;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Row of {@code global.plan} (migration V7). {@code customFields} is the jsonb object as text. */
@Entity
@Table(name = "plan")
class PlanEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String name;
    private BigDecimal price;
    private String currency;
    private String periodicity;
    @Column(name = "court_limit")
    private Integer courtLimit;
    @Column(name = "user_limit")
    private Integer userLimit;
    @ColumnTransformer(write = "CAST(? AS jsonb)")
    @Column(name = "custom_fields", nullable = false)
    private String customFields;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private long version;

    protected PlanEntity() {
    }

    PlanEntity(UUID id) {
        this.id = id;
    }

    void fill(String newName, BigDecimal newPrice, String newCurrency, String newPeriodicity, Integer newCourtLimit,
            Integer newUserLimit, String newCustomFields, String newStatus, long newVersion) {
        this.name = newName;
        this.price = newPrice;
        this.currency = newCurrency;
        this.periodicity = newPeriodicity;
        this.courtLimit = newCourtLimit;
        this.userLimit = newUserLimit;
        this.customFields = newCustomFields;
        this.status = newStatus;
        this.version = newVersion;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    BigDecimal price() {
        return price;
    }

    String currency() {
        return currency;
    }

    String periodicity() {
        return periodicity;
    }

    Integer courtLimit() {
        return courtLimit;
    }

    Integer userLimit() {
        return userLimit;
    }

    String customFields() {
        return customFields;
    }

    String status() {
        return status;
    }

    long version() {
        return version;
    }
}
