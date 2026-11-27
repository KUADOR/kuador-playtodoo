package com.kuador.platodo.api.audit.domain;

/**
 * Problem {@code code} answered to a refused support request, kept in its audit entry
 * ({@code contracts/platform-backoffice.openapi.yaml}, ADR-020).
 */
public enum DenialCode {

    /** An operator without {@code TENANT_DATA_ACCESS} tried to open a session. */
    CAPABILITY_REVOKED,
    /** The status of the complex does not allow the role of the caller. */
    TENANT_NOT_ACCESSIBLE,
    /** The reason is missing, blank or too long. */
    REASON_REQUIRED,
    /** A request on the host of a complex without a session that is valid for it right now. */
    SUPPORT_SESSION_INVALID
}
