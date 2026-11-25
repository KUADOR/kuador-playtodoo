package com.kuador.platodo.api.audit.application;

/** The caller is authenticated but is not the Kuador platform owner, the only role with access to complexes. */
public class NotPlatformOwnerException extends RuntimeException {

    public NotPlatformOwnerException() {
        super("Only the Kuador platform owner may access the data of a complex", null, false, false);
    }
}
