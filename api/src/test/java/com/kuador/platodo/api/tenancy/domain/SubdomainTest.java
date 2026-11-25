package com.kuador.platodo.api.tenancy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SubdomainTest {

    @ParameterizedTest
    @ValueSource(strings = {"abc", "canchas-el-sol", "elsol-norte-2", "123", "a1b"})
    void acceptsTheFormatOfAdr012(String candidate) {
        assertThat(Subdomain.parse(candidate)).contains(new Subdomain(candidate));
    }

    @Test
    void acceptsTheLongestLabel() {
        String sixtyThree = "a".repeat(63);

        assertThat(Subdomain.parse(sixtyThree)).isPresent();
    }

    @Test
    void comparesInLowerCase() {
        assertThat(Subdomain.parse("Canchas-El-SOL")).contains(new Subdomain("canchas-el-sol"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "-abc", "abc-", "a--b", "xn--abc", "a_b", "a.b", "a b", "ñandu", "canchas!"})
    void rejectsMalformedCandidates(String candidate) {
        assertThat(Subdomain.parse(candidate)).isEmpty();
    }

    @Test
    void rejectsALabelLongerThanSixtyThreeCharacters() {
        assertThat(Subdomain.parse("a".repeat(64))).isEmpty();
    }

    @Test
    void cannotBeBuiltFromAMalformedValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Subdomain("Upper"));
    }
}
