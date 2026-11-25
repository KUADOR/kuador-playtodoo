package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.Optional;

/**
 * The owner staff account of the complex in {@code TenantScope}, kept by Identity and Access in the tenant schema.
 * Every operation fails closed without a scope.
 */
public interface TenantOwnerAccounts {

    /**
     * Creates the owner account with {@code email} unless it already exists. It gets no usable password: the owner
     * sets one through the invitation of its own epic.
     */
    void createOwner(String email);

    boolean hasOwner();

    /** The e-mail of the owner, to tell it about the permanent deletion of the complex; empty without an owner. */
    Optional<String> ownerEmail();
}
