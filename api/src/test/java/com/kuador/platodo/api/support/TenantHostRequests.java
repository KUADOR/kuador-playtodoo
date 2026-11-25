package com.kuador.platodo.api.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

import com.kuador.platodo.api.tenancy.application.port.in.TenantId;

/**
 * Sends every MockMvc request of a test context to the host of one tenant, so the real subdomain resolution of
 * Tenancy runs it in that tenant. A request can still target another host with {@link #onHost(String)}.
 */
public final class TenantHostRequests implements MockMvcBuilderCustomizer {

    private final String host;

    public TenantHostRequests(TenantId tenant) {
        this.host = TestTenants.hostOf(tenant);
    }

    /** Sends the request to {@code host} (the server name the api sees). */
    public static RequestPostProcessor onHost(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.defaultRequest(get("/").with(onHost(host)));
    }
}
