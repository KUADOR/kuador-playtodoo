package com.kuador.platodo.api.identity.infrastructure.token;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * HS256 JWTs signed with the key of the environment. Claims: {@code sub}, {@code typ}, {@code tid} (only staff),
 * {@code role}, {@code iat} and {@code exp}. A token whose claims do not form a valid principal is not accepted.
 */
public final class JwtAccessTokens implements AccessTokens {

    static final String TYPE = "typ";
    static final String TENANT = "tid";
    static final String ROLE = "role";

    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;
    /** Tokens are issued and checked by the same api, so no clock skew is tolerated. */
    private static final Duration NO_CLOCK_SKEW = Duration.ZERO;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration timeToLive;
    private final Clock clock;

    public JwtAccessTokens(SecretKey key, Duration timeToLive, Clock clock) {
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(ALGORITHM).build();
        this.decoder = decoder(key, clock);
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(AuthenticatedPrincipal principal) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(principal.accountId().toString())
                .claim(TYPE, principal.type().name())
                .claim(ROLE, principal.role())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(timeToLive));
        principal.staffTenant().ifPresent(tenant -> claims.claim(TENANT, tenant.value()));
        JwsHeader header = JwsHeader.with(ALGORITHM).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, timeToLive);
    }

    @Override
    public Optional<AuthenticatedPrincipal> read(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException invalidSignatureOrExpired) {
            return Optional.empty();
        }
        return principalOf(jwt);
    }

    /** Empty when a claim is missing or malformed, even if the signature is valid. */
    private static Optional<AuthenticatedPrincipal> principalOf(Jwt jwt) {
        String subject = jwt.getSubject();
        String type = jwt.getClaimAsString(TYPE);
        String role = jwt.getClaimAsString(ROLE);
        String tenant = jwt.getClaimAsString(TENANT);
        if (subject == null || type == null || role == null || jwt.getExpiresAt() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AuthenticatedPrincipal(UUID.fromString(subject), AccountType.valueOf(type),
                    tenant == null ? null : new TenantId(tenant), role));
        } catch (IllegalArgumentException malformedClaim) {
            return Optional.empty();
        }
    }

    private static JwtDecoder decoder(SecretKey key, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
        JwtTimestampValidator expiry = new JwtTimestampValidator(NO_CLOCK_SKEW);
        expiry.setClock(clock);
        decoder.setJwtValidator(expiry);
        return decoder;
    }
}
