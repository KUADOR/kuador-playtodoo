package com.kuador.platodo.api.tenancy.application;

/**
 * A complex stuck in {@code PROVISIONING} cannot be activated because its owner account was never created: the
 * owner e-mail is not kept in the global schema, so a retry cannot recreate it.
 */
public class ProvisioningIncompleteException extends RuntimeException {

    public ProvisioningIncompleteException() {
        super("The owner account of the complex is missing; provisioning cannot be completed", null, false, false);
    }
}
