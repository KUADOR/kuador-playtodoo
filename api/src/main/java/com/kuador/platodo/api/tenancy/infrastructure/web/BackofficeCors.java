package com.kuador.platodo.api.tenancy.infrastructure.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS of the api (ADR-011): besides same-origin calls of the host itself, only the {@code backoffice} origin may
 * call it from a browser, with its bearer token and the support session header.
 */
final class BackofficeCors implements WebMvcConfigurer {

    private static final String API_PATHS = TenancyApiPaths.API + "/**";
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE"};

    private final String backofficeOrigin;

    BackofficeCors(String backofficeOrigin) {
        this.backofficeOrigin = backofficeOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API_PATHS)
                .allowedOrigins(backofficeOrigin)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
                        SupportRequests.SESSION_HEADER);
    }
}
