package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.util.UrlPathHelper;

import com.kuador.platodo.api.tenancy.application.RequestHost;
import com.kuador.platodo.api.tenancy.application.RequestHosts;
import com.kuador.platodo.api.tenancy.application.ResolveTenant;
import com.kuador.platodo.api.tenancy.application.TenantResolution;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * First filter of every api request (PLATODO-26 design, module Tenancy). It classifies the host, keeps each
 * operation on its host context, resolves the complex of a tenant host and runs the rest of the request inside its
 * {@link TenantScope}. Rejections never open a scope, so no tenant schema is touched:
 * <ul>
 *   <li>tenant operation on the global host, or global operation on a tenant host: 404 {@code RESOURCE_NOT_FOUND};</li>
 *   <li>malformed, unknown or retired subdomain, a complex that is not operating, or a foreign host:
 *       404 {@code TENANT_NOT_FOUND};</li>
 *   <li>suspended or archived complex: 403 {@code TENANT_UNAVAILABLE}.</li>
 * </ul>
 * The host is {@link HttpServletRequest#getServerName()}: the web server takes it from {@code X-Forwarded-Host} only
 * for requests of trusted proxies ({@code server.tomcat.remoteip.internal-proxies}).
 */
final class TenantResolutionFilter extends OncePerRequestFilter {

    private final RequestHosts hosts;
    private final ResolveTenant resolveTenant;
    private final HandlerExceptionResolver problemRenderer;

    TenantResolutionFilter(RequestHosts hosts, ResolveTenant resolveTenant, HandlerExceptionResolver problemRenderer) {
        this.hosts = hosts;
        this.resolveTenant = resolveTenant;
        this.problemRenderer = problemRenderer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean globalOperation =
                TenancyApiPaths.isGlobalContext(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
        switch (hosts.classify(request.getServerName())) {
            case RequestHost.Global() when globalOperation -> chain.doFilter(request, response);
            case RequestHost.Tenant(String label) when !globalOperation ->
                    continueInTenant(resolveTenant.resolve(label), request, response, chain);
            case RequestHost.Foreign() -> reject(TenancyProblem.TENANT_NOT_FOUND, request, response);
            default -> reject(TenancyProblem.RESOURCE_NOT_FOUND, request, response);
        }
    }

    private void continueInTenant(TenantResolution resolution, HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        switch (resolution) {
            case TenantResolution.Active active -> runInScope(active, request, response, chain);
            case TenantResolution.Unavailable() -> reject(TenancyProblem.TENANT_UNAVAILABLE, request, response);
            case TenantResolution.NotFound() -> reject(TenancyProblem.TENANT_NOT_FOUND, request, response);
        }
    }

    private static void runInScope(TenantResolution.Active active, HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        request.setAttribute(ActiveTenant.REQUEST_ATTRIBUTE, new ActiveTenant(active.displayName()));
        try {
            TenantScope.<Void, Exception>call(active.tenantId(), () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (ServletException | IOException | RuntimeException failure) {
            throw failure;
        } catch (Exception checkedFailure) {
            throw new ServletException(checkedFailure);
        }
    }

    private void reject(TenancyProblem problem, HttpServletRequest request, HttpServletResponse response)
            throws ServletException {
        TenantRequestRejectedException rejection = new TenantRequestRejectedException(problem);
        if (problemRenderer.resolveException(request, response, null, rejection) == null) {
            throw new ServletException("No handler renders tenant resolution problems", rejection);
        }
    }
}
