package com.kuador.platodo.api.tenancy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.kuador.platodo.api.tenancy.domain.SubdomainRules.Rejection;

class SubdomainRulesTest {

    private final SubdomainRules rules = new SubdomainRules(List.of("www", " Admin ", "api"));

    @Test
    void acceptsAWellFormedFreeNameInLowerCase() {
        assertThat(rules.accept("Canchas-El-Sol")).isEqualTo(new Subdomain("canchas-el-sol"));
        assertThat(rules.rejectionOf("canchas-el-sol")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "-abc", "abc-", "a--b", "xn--abc", "a_b", "ñandu"})
    void rejectsMalformedNamesAsInvalid(String candidate) {
        assertThat(rules.rejectionOf(candidate)).contains(Rejection.INVALID);
        assertThatThrownBy(() -> rules.accept(candidate))
                .isInstanceOfSatisfying(SubdomainRejectedException.class,
                        rejected -> assertThat(rejected.rejection()).isEqualTo(Rejection.INVALID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"www", "WWW", "admin", "api"})
    void rejectsReservedNamesWhateverTheirCase(String candidate) {
        assertThat(rules.rejectionOf(candidate)).contains(Rejection.RESERVED);
        assertThatThrownBy(() -> rules.accept(candidate))
                .isInstanceOfSatisfying(SubdomainRejectedException.class,
                        rejected -> assertThat(rejected.rejection()).isEqualTo(Rejection.RESERVED));
    }

    @Test
    void aNameThatOnlyStartsLikeAReservedOneIsAllowed() {
        assertThat(rules.rejectionOf("www-sol")).isEmpty();
    }
}
