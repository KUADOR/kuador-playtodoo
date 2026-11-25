package com.kuador.platodo.api.tenancy.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.kuador.platodo.api.support.TenantDatabase;
import com.kuador.platodo.api.support.TestTenants;
import com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantMigrations;

/**
 * The host behind a proxy comes from {@code X-Forwarded-Host} only when the request arrives from a configured proxy.
 * Runs on the real web server (the forwarded headers are processed by Tomcat, not by MockMvc) and calls it by IP, so
 * that the {@code Host} header alone never names a complex.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TenantDatabase.SpringConnection.class)
class ForwardedHostIT {

    private static final String FORWARDED_HOST = "X-Forwarded-Host";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;
    @Autowired
    private TenantMigrations migrations;

    @BeforeEach
    void provisionTenantA() {
        TestTenants.provision(migrations, TestTenants.A);
    }

    @Nested
    @TestPropertySource(properties = "server.tomcat.remoteip.internal-proxies=127.0.0.0/8, ::1/128")
    class FromATrustedProxy {

        @Test
        void theForwardedHostResolvesTheComplex() throws Exception {
            HttpResponse<String> response = getTenantState(TestTenants.hostOf(TestTenants.A) + ":8000");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"ACTIVE\"");
        }

        @Test
        void withoutAForwardedHostTheHostHeaderIsUsed() throws Exception {
            HttpResponse<String> response = getTenantState(null);

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("\"code\":\"TENANT_NOT_FOUND\"");
        }
    }

    @Nested
    @TestPropertySource(properties = "server.tomcat.remoteip.internal-proxies=192.0.2.1/32")
    class FromAnUntrustedClient {

        @Test
        void theForwardedHostIsIgnored() throws Exception {
            HttpResponse<String> response = getTenantState(TestTenants.hostOf(TestTenants.A));

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("\"code\":\"TENANT_NOT_FOUND\"");
        }
    }

    private HttpResponse<String> getTenantState(String forwardedHost) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + TenancyApiPaths.TENANT_STATE));
        if (forwardedHost != null) {
            request.header(FORWARDED_HOST, forwardedHost);
        }
        return HTTP.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
