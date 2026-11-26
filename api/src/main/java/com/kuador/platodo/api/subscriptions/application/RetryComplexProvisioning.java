package com.kuador.platodo.api.subscriptions.application;

import java.util.UUID;

import com.kuador.platodo.api.identity.application.port.in.CapabilityGuard;
import com.kuador.platodo.api.identity.domain.Capability;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantOnboarding;

/**
 * {@code POST /complexes/{complexId}/provisioning-retries}: capability {@code COMPLEX_CREATE}. Runs the retry before
 * answering, so the detail already says whether it ended {@code ACTIVE} or {@code PROVISIONING_FAILED} again.
 */
public final class RetryComplexProvisioning {

    private final CapabilityGuard guard;
    private final TenantOnboarding onboarding;
    private final ComplexDetails details;

    public RetryComplexProvisioning(CapabilityGuard guard, TenantOnboarding onboarding, ComplexDetails details) {
        this.guard = guard;
        this.onboarding = onboarding;
        this.details = details;
    }

    /**
     * @throws ComplexNotFoundException if no complex has this id
     * @throws com.kuador.platodo.api.tenancy.application.port.in.ProvisioningNotFailedException if it is not waiting
     *         for a retry
     */
    public ComplexDetail retry(UUID complexId) {
        guard.require(Capability.COMPLEX_CREATE);
        details.requireExists(complexId);
        onboarding.retryProvisioning(new TenantId(complexId.toString()));
        return details.of(complexId);
    }
}
