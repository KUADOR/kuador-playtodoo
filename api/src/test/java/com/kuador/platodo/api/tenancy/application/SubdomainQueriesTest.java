package com.kuador.platodo.api.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.kuador.platodo.api.tenancy.application.CheckSubdomainAvailability.Availability;
import com.kuador.platodo.api.tenancy.application.CheckSubdomainAvailability.Reason;
import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;

/** Suggestion and availability of ADR-012 over an in-memory registry of used subdomains. */
class SubdomainQueriesTest {

    private final UsedSubdomains used = new UsedSubdomains();
    private final SubdomainRules rules = new SubdomainRules(List.of("www", "complejo"));
    private final SuggestSubdomain suggest = new SuggestSubdomain(rules, used);
    private final CheckSubdomainAvailability availability = new CheckSubdomainAvailability(rules, used);

    @Test
    void suggestsTheNameItselfWhenItIsFree() {
        assertThat(suggest.suggest("Canchas El Sol")).isEqualTo(new Subdomain("canchas-el-sol"));
    }

    @Test
    void suggestsTheFirstFreeNumberWhenTheNameIsTaken() {
        used.add("canchas-el-sol", "canchas-el-sol-2");

        assertThat(suggest.suggest("Canchas El Sol")).isEqualTo(new Subdomain("canchas-el-sol-3"));
        assertThat(used.lookups).isEqualTo(1);
    }

    @Test
    void skipsReservedCandidates() {
        assertThat(suggest.suggest("!!!")).isEqualTo(new Subdomain("complejo-2"));
    }

    @Test
    void looksAtTheNextPageWhenTheFirstOneIsFull() {
        IntStream.range(0, SuggestSubdomain.PAGE_SIZE).mapToObj(n -> n == 0 ? "sol" : "sol-" + (n + 1))
                .forEach(used::add);

        assertThat(suggest.suggest("Sol")).isEqualTo(new Subdomain("sol-" + (SuggestSubdomain.PAGE_SIZE + 1)));
        assertThat(used.lookups).isEqualTo(2);
    }

    @Test
    void availabilityChecksFormatThenReservedThenUse() {
        used.add("canchas-el-sol");

        assertThat(availability.check("Canchas-El-Sol")).isEqualTo(new Availability("canchas-el-sol", Reason.TAKEN));
        assertThat(availability.check("WWW")).isEqualTo(new Availability("www", Reason.RESERVED));
        assertThat(availability.check("a--b")).isEqualTo(new Availability("a--b", Reason.INVALID));
        Availability free = availability.check("la-esquina");
        assertThat(free.available()).isTrue();
        assertThat(free.reason()).isNull();
    }

    private static final class UsedSubdomains implements SubdomainUsage {

        private final Set<String> values = new HashSet<>();
        private int lookups;

        void add(String... subdomains) {
            values.addAll(List.of(subdomains));
        }

        @Override
        public Set<Subdomain> usedAmong(Collection<Subdomain> candidates) {
            lookups++;
            return candidates.stream().filter(candidate -> values.contains(candidate.value()))
                    .collect(Collectors.toSet());
        }
    }
}
