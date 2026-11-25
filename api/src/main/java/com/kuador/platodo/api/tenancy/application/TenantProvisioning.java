package com.kuador.platodo.api.tenancy.application;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.TenantOwnerAccounts;
import com.kuador.platodo.api.tenancy.application.port.out.TenantSchemas;

/**
 * Provisioning of a registered complex: its schema {@code t_<uuid>} with the tenant migrations and its owner staff
 * account, both inside the complex's own schema. Every step is idempotent, so a failed provisioning can be retried.
 */
final class TenantProvisioning {

    private final TenantSchemas schemas;
    private final TenantOwnerAccounts owners;

    TenantProvisioning(TenantSchemas schemas, TenantOwnerAccounts owners) {
        this.schemas = schemas;
        this.owners = owners;
    }

    /** First provisioning, at registration. */
    void provision(TenantId tenant, String ownerEmail) {
        schemas.provision(tenant);
        TenantScope.run(tenant, () -> owners.createOwner(ownerEmail));
    }

    /**
     * Retry of a failed provisioning: brings the schema up to date and checks that the owner exists.
     *
     * @throws ProvisioningIncompleteException if the owner account was never created
     */
    void retry(TenantId tenant) {
        schemas.provision(tenant);
        if (!TenantScope.call(tenant, owners::hasOwner)) {
            throw new ProvisioningIncompleteException();
        }
    }
}
