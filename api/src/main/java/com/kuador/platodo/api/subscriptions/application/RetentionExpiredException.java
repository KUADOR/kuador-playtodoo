package com.kuador.platodo.api.subscriptions.application;

/**
 * A suspended subscription cannot be reactivated: the retention of its complex is over (purge date reached, or the
 * complex is being or was purged), so its data is gone or about to be (ADR-014).
 */
public class RetentionExpiredException extends RuntimeException {

    public RetentionExpiredException() {
        super("The purge date has passed; the complex can no longer be reactivated", null, false, false);
    }
}
