package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.ResolveTenant;
import com.kuador.platodo.api.tenancy.application.port.in.SupportGrants;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization;
import com.kuador.platodo.api.tenancy.application.port.out.TenantDomains;

/**
 * Tenant resolution by subdomain for every api request (ADR-002, ADR-012), the guard of Kuador support on a complex
 * and the CORS of the api (ADR-011).
 */
@Configuration(proxyBeanMethods = false)
class TenancyWebConfiguration {

    /**
     * Before any other application filter, so that nothing (security included) runs on a request whose complex is
     * not resolved. Spring Boot's character encoding filter keeps the very first place.
     */
    static final int FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    private static final String API_URL_PATTERN = TenancyApiPaths.API + "/*";
    private static final String API_PATH_PATTERN = TenancyApiPaths.API + "/**";
    private static final String HANDLER_EXCEPTION_RESOLVER = "handlerExceptionResolver";

    @Bean
    RequestHosts requestHosts(@Value("${playtodoo.root-domain}") String rootDomain) {
        return new RequestHosts(rootDomain);
    }

    @Bean
    TenantUrls tenantUrls(@Value("${playtodoo.tenant-url-pattern}") String pattern) {
        return new TenantUrls(pattern);
    }

    @Bean
    ResolveTenant resolveTenant(TenantDomains domains) {
        return new ResolveTenant(domains);
    }

    @Bean
    FilterRegistrationBean<TenantResolutionFilter> tenantResolutionFilter(RequestHosts hosts,
            ResolveTenant resolveTenant,
            @Qualifier(HANDLER_EXCEPTION_RESOLVER) HandlerExceptionResolver problemRenderer) {
        FilterRegistrationBean<TenantResolutionFilter> registration =
                new FilterRegistrationBean<>(new TenantResolutionFilter(hosts, resolveTenant, problemRenderer));
        registration.addUrlPatterns(API_URL_PATTERN);
        registration.setOrder(FILTER_ORDER);
        return registration;
    }

    @Bean
    WebMvcConfigurer supportAccessGuard(SupportAccessAuthorization authorization) {
        SupportAccessGuard guard = new SupportAccessGuard(authorization);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(guard).addPathPatterns(API_PATH_PATTERN);
            }
        };
    }

    /** The grants {@link SupportAccessGuard} records, for the modules whose operations support may use. */
    @Bean
    SupportGrants supportGrants() {
        return new RequestSupportGrants();
    }

    @Bean
    WebMvcConfigurer backofficeCors(@Value("${playtodoo.cors.backoffice-origin}") String backofficeOrigin) {
        return new BackofficeCors(backofficeOrigin);
    }
}
