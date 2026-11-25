package com.kuador.platodo.api.tenancy.application.port.out;

/**
 * The owner staff account of the complex in {@code TenantScope}, kept by Identity and Access in the tenant schema.
 * Both operations fail closed without a scope.
 */
public interface TenantOwnerAccounts {

    /**
     * Creates the owner account with {@code email} unless it already exists. It gets no usable password: the owner
     * sets one through the invitation of its own epic.
     */
    void createOwner(String email);

    boolean hasOwner();
}
