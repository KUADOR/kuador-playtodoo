package com.kuador.platodo.api.subscriptions.domain;

/** A draft cannot be assigned; a published plan can. Unpublishing is out of scope (ADR-018). */
public enum PlanStatus {
    DRAFT,
    PUBLISHED
}
