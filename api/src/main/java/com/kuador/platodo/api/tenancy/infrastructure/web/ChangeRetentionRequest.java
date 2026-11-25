package com.kuador.platodo.api.tenancy.infrastructure.web;

import com.kuador.platodo.api.tenancy.domain.Tenant;

/** {@code ChangeRetentionRequest} of the contract. */
record ChangeRetentionRequest(Integer days) {

    /**
     * Technical ceiling only, so that the purge date always fits a PostgreSQL timestamp. The business maximum is
     * still [POR DEFINIR] (ADR-013).
     */
    static final int MAX_DAYS = 36_500;

    /** @throws InvalidRequestException if {@code days} is missing or out of range */
    ChangeRetentionRequest {
        if (days == null || days < Tenant.MIN_RETENTION_DAYS || days > MAX_DAYS) {
            throw new InvalidRequestException(
                    "days must be between " + Tenant.MIN_RETENTION_DAYS + " and " + MAX_DAYS);
        }
    }
}
