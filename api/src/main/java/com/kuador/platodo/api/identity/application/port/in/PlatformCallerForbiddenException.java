package com.kuador.platodo.api.identity.application.port.in;

/**
 * Not a platform token, or a platform role that does not allow the operation ({@code 403 FORBIDDEN}), such as an
 * operator calling an operation reserved to the SUPER_ADMIN.
 */
public final class PlatformCallerForbiddenException extends RuntimeException {

    public PlatformCallerForbiddenException() {
        super("The caller may not use this platform operation");
    }
}
