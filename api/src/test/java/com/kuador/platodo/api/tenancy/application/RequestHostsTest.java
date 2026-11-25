package com.kuador.platodo.api.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RequestHostsTest {

    private final RequestHosts hosts = new RequestHosts("playtodoo.com");

    @Test
    void theApiLabelIsTheGlobalHost() {
        assertThat(hosts.classify("api.playtodoo.com")).isEqualTo(new RequestHost.Global());
    }

    @Test
    void anyOtherSingleLabelIsATenantHostToResolve() {
        assertThat(hosts.classify("canchas-el-sol.playtodoo.com"))
                .isEqualTo(new RequestHost.Tenant("canchas-el-sol"));
    }

    @Test
    void comparesInLowerCaseAndAcceptsAFullyQualifiedHost() {
        assertThat(hosts.classify("Canchas-El-Sol.PLAYTODOO.com.")).isEqualTo(new RequestHost.Tenant("canchas-el-sol"));
        assertThat(hosts.classify("API.playtodoo.com")).isEqualTo(new RequestHost.Global());
    }

    @Test
    void passesMalformedLabelsOnForValidation() {
        assertThat(hosts.classify("-x.playtodoo.com")).isEqualTo(new RequestHost.Tenant("-x"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"playtodoo.com", ".playtodoo.com", "a.b.playtodoo.com", "evilplaytodoo.com",
        "playtodoo.com.evil.com", "localhost", "example.com"})
    void everyOtherHostIsForeign(String host) {
        assertThat(hosts.classify(host)).isEqualTo(new RequestHost.Foreign());
    }

    @Test
    void worksWithTheLocalRootDomain() {
        RequestHosts local = new RequestHosts("localhost");

        assertThat(local.classify("elsol.localhost")).isEqualTo(new RequestHost.Tenant("elsol"));
        assertThat(local.classify("api.localhost")).isEqualTo(new RequestHost.Global());
        assertThat(local.classify("localhost")).isEqualTo(new RequestHost.Foreign());
    }

    @Test
    void needsARootDomain() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RequestHosts(" "));
    }
}
