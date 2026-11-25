package com.kuador.platodo.api.identity.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.out.AccessTokens.IssuedToken;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

import tools.jackson.databind.json.JsonMapper;

class JwtAccessTokensTest {

    private static final Instant NOW = Instant.parse("2026-12-04T18:00:00Z");
    private static final Duration TIME_TO_LIVE = Duration.ofHours(1);
    private static final TenantId TENANT = new TenantId("0a0a0a0a-0000-4000-8000-00000000000a");
    private static final SecretKey KEY = keyOf("first-test-key-with-at-least-256-bits!!");
    private static final SecretKey OTHER_KEY = keyOf("other-test-key-with-at-least-256-bits!!");

    private final JwtAccessTokens tokens = tokensAt(KEY, NOW);

    @Test
    void staffTokenCarriesTheContractClaims() {
        UUID staffId = UUID.randomUUID();
        AuthenticatedPrincipal staff = AuthenticatedPrincipal.staff(staffId, TENANT, "OWNER");

        IssuedToken token = tokens.issue(staff);

        assertThat(token.timeToLive()).isEqualTo(TIME_TO_LIVE);
        assertThat(tokens.read(token.value())).contains(staff);
        assertThat(claimsOf(token.value())).containsEntry("sub", staffId.toString())
                .containsEntry("typ", "STAFF")
                .containsEntry("tid", TENANT.value())
                .containsEntry("role", "OWNER")
                .containsKeys("iat", "exp");
    }

    @Test
    void playerTokenHasNoTenantClaim() {
        AuthenticatedPrincipal player = AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLAYER, "PLAYER");

        String token = tokens.issue(player).value();

        assertThat(tokens.read(token)).contains(player);
        assertThat(claimsOf(token)).doesNotContainKey("tid").containsEntry("typ", "PLAYER");
    }

    @Test
    void expiredTokenIsNotAccepted() {
        String token = tokens.issue(anyPlayer()).value();

        assertThat(tokensAt(KEY, NOW.plus(TIME_TO_LIVE).minusSeconds(1)).read(token)).isPresent();
        assertThat(tokensAt(KEY, NOW.plus(TIME_TO_LIVE).plusSeconds(1)).read(token)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsNotAccepted() {
        String foreign = tokensAt(OTHER_KEY, NOW).issue(anyPlayer()).value();

        assertThat(tokens.read(foreign)).isEmpty();
    }

    @Test
    void tamperedClaimsAreNotAccepted() {
        String[] parts = tokens.issue(AuthenticatedPrincipal.staff(UUID.randomUUID(), TENANT, "STAFF")).value()
                .split("\\.");
        String otherTenant = encode(decode(parts[1]).replace(TENANT.value(), "0b0b0b0b-0000-4000-8000-00000000000b"));

        assertThat(tokens.read(parts[0] + "." + otherTenant + "." + parts[2])).isEmpty();
    }

    @Test
    void unsignedOrMalformedTokensAreNotAccepted() {
        String unsignedHeader = encode("{\"alg\":\"none\"}");
        String payload = tokens.issue(anyPlayer()).value().split("\\.")[1];

        assertThat(tokens.read(unsignedHeader + "." + payload + ".")).isEmpty();
        assertThat(tokens.read("not-a-jwt")).isEmpty();
    }

    @Test
    void theSigningKeyMustBeBase64WithAtLeast256Bits() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatThrownBy(() -> new AccessTokenProperties(shortKey, TIME_TO_LIVE).hmacKey())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("256 bits");
        assertThatThrownBy(() -> new AccessTokenProperties("not base64!", TIME_TO_LIVE).hmacKey())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Base64");
        assertThatThrownBy(() -> new AccessTokenProperties(null, TIME_TO_LIVE))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("required");
        assertThatThrownBy(() -> new AccessTokenProperties(
                Base64.getEncoder().encodeToString(new byte[32]), Duration.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AuthenticatedPrincipal anyPlayer() {
        return AuthenticatedPrincipal.global(UUID.randomUUID(), AccountType.PLAYER, "PLAYER");
    }

    private static JwtAccessTokens tokensAt(SecretKey key, Instant now) {
        return new JwtAccessTokens(key, TIME_TO_LIVE, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static SecretKey keyOf(String text) {
        return new AccessTokenProperties(
                Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)), TIME_TO_LIVE).hmacKey();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> claimsOf(String token) {
        return JsonMapper.builder().build().readValue(decode(token.split("\\.")[1]), Map.class);
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
