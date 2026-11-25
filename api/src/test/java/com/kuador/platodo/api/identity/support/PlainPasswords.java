package com.kuador.platodo.api.identity.support;

import com.kuador.platodo.api.identity.application.port.out.PasswordHashes;

/** Compares passwords as stored, and counts the comparisons spent on unknown accounts. */
public final class PlainPasswords implements PasswordHashes {

    private int unknownAccountChecks;

    @Override
    public String hash(String rawPassword) {
        return rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword.equals(passwordHash);
    }

    @Override
    public void matchNothing(String rawPassword) {
        unknownAccountChecks++;
    }

    public int unknownAccountChecks() {
        return unknownAccountChecks;
    }
}
