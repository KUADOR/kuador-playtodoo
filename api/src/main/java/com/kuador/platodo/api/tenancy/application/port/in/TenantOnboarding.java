package com.kuador.platodo.api.tenancy.application.port.in;

import java.util.Objects;

import com.kuador.platodo.api.tenancy.domain.TenantStatus;

/**
 * Registration of a complex for the module that owns the registration request (Plans and subscriptions, ADR-014).
 * Authorizing the caller is that module's job: these operations check no caller. Only Tenancy creates a schema
 * outside a request resolved by subdomain (exception E1 of the PLATODO-27 design): always the one named after the new
 * tenant id, never another one.
 */
public interface TenantOnboarding {

    /**
     * Records a new complex in {@code PROVISIONING} with its subdomain and owner e-mail. It joins the surrounding
     * global transaction, so the caller can record its own rows with it and nothing is kept if any of them fails.
     * No schema is created yet.
     *
     * @throws SubdomainUnavailableException if the subdomain breaks the rules, is reserved, or is or was used
     */
    TenantId record(NewTenant tenant);

    /**
     * Creates and migrates the schema of a recorded complex and its owner account, then activates it. Must run after
     * the transaction of {@link #record} committed. If provisioning fails the complex is left in
     * {@code PROVISIONING_FAILED} for {@link #retryProvisioning}.
     *
     * @return {@link TenantStatus#ACTIVE} or {@link TenantStatus#PROVISIONING_FAILED}
     */
    TenantStatus provision(TenantId tenant);

    /**
     * Retries the provisioning of a complex in {@code PROVISIONING_FAILED}: it goes back to {@code PROVISIONING} and
     * {@link #provision} runs again. Idempotent: the schema and owner already created are kept.
     *
     * @return {@link TenantStatus#ACTIVE} or {@link TenantStatus#PROVISIONING_FAILED}
     * @throws ProvisioningNotFailedException if the complex does not exist or is not waiting for a retry
     */
    TenantStatus retryProvisioning(TenantId tenant);

    /** What a registration gives Tenancy. {@code displayName} and {@code ownerEmail} come already validated. */
    record NewTenant(String displayName, String subdomain, String ownerEmail) {

        public NewTenant {
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(subdomain, "subdomain");
            Objects.requireNonNull(ownerEmail, "ownerEmail");
        }
    }
}
