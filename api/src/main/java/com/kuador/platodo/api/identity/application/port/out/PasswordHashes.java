package com.kuador.platodo.api.identity.application.port.out;

/** Hashes passwords and verifies them against their stored hashes. */
public interface PasswordHashes {

    /** The hash to store for {@code rawPassword}; it names its algorithm, so it can be upgraded later. */
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Spends the same work as {@link #matches} when there is no account, so the response time does not tell whether
     * an e-mail has an account.
     */
    void matchNothing(String rawPassword);
}
