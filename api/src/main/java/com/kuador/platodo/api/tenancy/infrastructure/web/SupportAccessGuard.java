package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Objects;
import java.util.Optional;

import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization;
import com.kuador.platodo.api.tenancy.application.port.out.SupportAccessAuthorization.SupportAccessAttempt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Kuador support on the host of a complex (ADR-011 D6 and D7, ADR-020), before any handler runs:
 * <ul>
 *   <li>a request of a {@code PLATFORM} account runs only when {@link SupportAccessAuthorization} allows it, which also
 *       audits it; then {@link RequestSupportGrants} records the grant for the modules it reaches. Otherwise
 *       {@code 403 SUPPORT_SESSION_INVALID};</li>
 *   <li>any other request on a suspended or archived complex, which the tenant filter only let through because it
 *       announced a support session, answers {@code 403 TENANT_UNAVAILABLE}, as without the header.</li>
 * </ul>
 * A CORS preflight runs no operation and is left to the CORS configuration.
 *
 * <p>It is a handler interceptor, not a filter, because the audit stores the route template of the chosen handler,
 * which only exists once the handler is mapped.
 */
final class SupportAccessGuard implements HandlerInterceptor {

    /** Route template audited when no handler pattern was matched. */
    static final String UNMATCHED_ROUTE = "[unmatched]";

    private final SupportAccessAuthorization authorization;

    SupportAccessGuard(SupportAccessAuthorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<TenantId> tenant = TenantScope.current();
        if (tenant.isEmpty() || CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        ResolvedTenant resolved = Objects.requireNonNull(
                (ResolvedTenant) request.getAttribute(ResolvedTenant.REQUEST_ATTRIBUTE),
                "A tenant scope on a web request is only opened by the tenant resolution filter");
        Optional<AuthenticatedPrincipal> platformUser =
                CurrentPrincipal.current().filter(principal -> principal.is(AccountType.PLATFORM));
        if (platformUser.isEmpty()) {
            if (!resolved.isActive()) {
                throw new TenantRequestRejectedException(TenancyProblem.TENANT_UNAVAILABLE);
            }
            return true;
        }
        if (!authorization.authorize(attemptOf(platformUser.get(), tenant.get(), request))) {
            throw new TenantRequestRejectedException(TenancyProblem.SUPPORT_SESSION_INVALID);
        }
        RequestSupportGrants.grant(request, platformUser.get().accountId(), tenant.get());
        return true;
    }

    private static SupportAccessAttempt attemptOf(AuthenticatedPrincipal platformUser, TenantId tenant,
            HttpServletRequest request) {
        String routeTemplate = routeTemplateOf(request);
        return new SupportAccessAttempt(
                platformUser.accountId(),
                tenant,
                SupportRequests.sessionHeader(request),
                request.getMethod(),
                routeTemplate,
                TenantHostOperations.operationIdOf(request.getMethod(), routeTemplate));
    }

    private static String routeTemplateOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? UNMATCHED_ROUTE : pattern.toString();
    }
}
