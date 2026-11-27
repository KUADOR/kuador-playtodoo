package com.kuador.platodo.api.subscriptions.infrastructure.web;

import java.util.List;

import com.kuador.platodo.api.subscriptions.domain.FieldError;
import com.kuador.platodo.api.tenancy.domain.Tenant;

/** Schema {@code ChangeRetentionRequest}; both properties are required. */
record ChangeRetentionRequest(Integer retentionDays, Long version) {

    /**
     * Technical ceiling only, so that the purge date always fits a PostgreSQL timestamp. The business maximum is
     * still [POR DEFINIR: maximum retention days, if any] (ADR-013).
     */
    static final int MAX_RETENTION_DAYS = 36_500;

    private static final String RETENTION_DAYS = "retentionDays";

    /**
     * @throws InvalidSubscriptionsRequestException if a property is missing
     * @throws InvalidRetentionRequestException     if {@code retentionDays} is out of range
     */
    ChangeRetentionRequest requireValid() {
        if (retentionDays == null || version == null || version < 0) {
            throw new InvalidSubscriptionsRequestException("`retentionDays` and `version` (>= 0) are required.");
        }
        if (retentionDays < Tenant.MIN_RETENTION_DAYS) {
            throw new InvalidRetentionRequestException(List.of(new FieldError(RETENTION_DAYS,
                    FieldError.Code.NOT_POSITIVE)));
        }
        if (retentionDays > MAX_RETENTION_DAYS) {
            throw new InvalidRetentionRequestException(List.of(new FieldError(RETENTION_DAYS,
                    FieldError.Code.NOT_ALLOWED)));
        }
        return this;
    }
}
