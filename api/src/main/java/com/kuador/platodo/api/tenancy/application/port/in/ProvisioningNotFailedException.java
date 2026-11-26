package com.kuador.platodo.api.tenancy.application.port.in;

/** A provisioning retry was asked for a complex that is not in {@code PROVISIONING_FAILED}. */
public class ProvisioningNotFailedException extends RuntimeException {

    public ProvisioningNotFailedException() {
        super("The tenant is not waiting for a provisioning retry", null, false, false);
    }
}
