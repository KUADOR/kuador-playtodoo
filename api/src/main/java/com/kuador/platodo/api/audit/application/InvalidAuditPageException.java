package com.kuador.platodo.api.audit.application;

/** The requested page of the audit is out of the contract limits. */
public class InvalidAuditPageException extends RuntimeException {

    public InvalidAuditPageException() {
        super("Page must be 0 or more and size between 1 and " + ListAccessAudit.MAX_PAGE_SIZE, null, false, false);
    }
}
