package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import org.springframework.transaction.support.TransactionOperations;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantRetention;

/**
 * {@code PUT /complexes/{complexId}/retention}: SUPER_ADMIN only (ADR-014, ADR-017). In one global transaction, with
 * the complex locked at the version the caller read, Tenancy recomputes the purge date of the suspended or archived
 * complex from the date its retention started. The subscription does not change.
 */
public final class ChangeComplexRetention {

    private final CapabilityGuard guard;
    private final ComplexLocks locks;
    private final TenantRetention retention;
    private final ComplexDetails details;
    private final TransactionOperations globalTransaction;

    public ChangeComplexRetention(CapabilityGuard guard, ComplexLocks locks, TenantRetention retention,
            ComplexDetails details, TransactionOperations globalTransaction) {
        this.guard = guard;
        this.locks = locks;
        this.retention = retention;
        this.details = details;
        this.globalTransaction = globalTransaction;
    }

    /**
     * @param retentionDays already validated against the contract limits
     * @throws ComplexNotFoundException if no complex has this id
     * @throws StaleVersionException    if {@code expectedVersion} is not the current version of the complex
     * @throws com.kuador.platodo.api.tenancy.domain.NotInRetentionException   if the complex is neither suspended nor
     *                                                                          archived
     * @throws com.kuador.platodo.api.tenancy.domain.RetentionInvalidException if the new purge date would not be in the
     *                                                                          future
     */
    public ComplexDetail change(UUID complexId, int retentionDays, long expectedVersion) {
        guard.requireSuperAdmin();
        return globalTransaction.execute(status -> {
            locks.lockAt(complexId, expectedVersion);
            retention.changeRetention(new TenantId(complexId.toString()), retentionDays);
            return details.of(complexId);
        });
    }
}
