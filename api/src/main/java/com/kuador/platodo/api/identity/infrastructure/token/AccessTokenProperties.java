package com.kuador.platodo.api.identity.infrastructure.token;

import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the access tokens ({@value #PREFIX}.*).
 *
 * @param signingKey Base64 HMAC-SHA256 key, at least 256 bits. Set per environment
 *                   ({@code PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY}); it is a secret and never lives in the repository.
 * @param timeToLive lifetime of an issued token; a security setting, not a business rule
 */
@ConfigurationProperties(AccessTokenProperties.PREFIX)
public record AccessTokenProperties(String signingKey, Duration timeToLive) {

    public static final String PREFIX = "playtodoo.identity.token";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MINIMUM_KEY_BYTES = 32;

    public AccessTokenProperties {
        if (signingKey == null || signingKey.isBlank()) {
            throw new IllegalStateException(PREFIX + ".signing-key is required (PLAYTODOO_IDENTITY_TOKEN_SIGNING_KEY)");
        }
        Objects.requireNonNull(timeToLive, PREFIX + ".time-to-live");
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            throw new IllegalStateException(PREFIX + ".time-to-live must be positive");
        }
    }

    /** The HMAC key; fails when the value is not Base64 or is shorter than 256 bits. */
    public SecretKey hmacKey() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(signingKey.strip());
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalStateException(PREFIX + ".signing-key must be Base64", notBase64);
        }
        if (key.length < MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(PREFIX + ".signing-key must have at least 256 bits");
        }
        return new SecretKeySpec(key, HMAC_SHA256);
    }
}
