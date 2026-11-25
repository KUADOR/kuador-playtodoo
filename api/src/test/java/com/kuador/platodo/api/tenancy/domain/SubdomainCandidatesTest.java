package com.kuador.platodo.api.tenancy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubdomainCandidatesTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Canchas El Sol         | canchas-el-sol",
        "Fútbol 5 · Ñandú       | futbol-5-nandu",
        "  --La   Esquina!!  -- | la-esquina",
        "ÁRENA                  | arena",
        "!!!                    | complejo",
        "A                      | a-complejo",
        "F5                     | f5-complejo"})
    void theFirstCandidateComesFromTheName(String name, String expected) {
        assertThat(first(name)).isEqualTo(new Subdomain(expected));
    }

    @Test
    void theNextCandidatesAreNumberedFromTwo() {
        assertThat(SubdomainCandidates.from("Canchas El Sol").page(0, 3)).containsExactly(
                new Subdomain("canchas-el-sol"), new Subdomain("canchas-el-sol-2"), new Subdomain("canchas-el-sol-3"));
        assertThat(SubdomainCandidates.from("Canchas El Sol").page(3, 1))
                .containsExactly(new Subdomain("canchas-el-sol-4"));
    }

    @Test
    void longNamesAreCutToSixtyThreeCharactersWithoutHyphensAtTheEnds() {
        String name = "a".repeat(62) + " b";

        List<Subdomain> candidates = SubdomainCandidates.from(name).page(0, 2);

        assertThat(candidates.get(0).value()).isEqualTo("a".repeat(62));
        assertThat(candidates.get(1).value()).isEqualTo("a".repeat(61) + "-2");
    }

    @Test
    void aNumberNeverLeavesTwoHyphensTogether() {
        String name = "a".repeat(60) + " bcd";

        Subdomain second = SubdomainCandidates.from(name).page(1, 1).getFirst();

        assertThat(second.value()).isEqualTo("a".repeat(60) + "-2");
    }

    private static Subdomain first(String name) {
        return SubdomainCandidates.from(name).page(0, 1).getFirst();
    }
}
