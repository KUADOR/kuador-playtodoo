package com.kuador.platodo.api.identity.application.port.out;

/** Verifies passwords against their stored hashes. */
public interface PasswordHashes {

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Spends the same work as {@link #matches} when there is no account, so the response time does not tell whether
     * an e-mail has an account.
     */
    void matchNothing(String rawPassword);
}
