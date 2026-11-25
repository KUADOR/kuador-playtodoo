package com.kuador.platodo.api.identity.infrastructure.crypto;

import java.util.UUID;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kuador.platodo.api.identity.application.port.out.PasswordHashes;

/**
 * Spring Security's delegating encoder: hashes carry their algorithm ({@code {bcrypt}...}), so the algorithm can be
 * upgraded later without invalidating stored passwords.
 */
public final class DelegatingPasswordHashes implements PasswordHashes {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    /** Hash of a random value nobody knows, compared against when there is no account. */
    private final String unknownAccountHash = encoder.encode(UUID.randomUUID().toString());

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void matchNothing(String rawPassword) {
        encoder.matches(rawPassword, unknownAccountHash);
    }
}
