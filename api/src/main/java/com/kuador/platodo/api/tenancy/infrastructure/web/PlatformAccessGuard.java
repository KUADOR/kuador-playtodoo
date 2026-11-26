package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Optional;

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.PlatformAccessAuthorization;
import com.kuador.platodo.api.tenancy.application.port.out.PlatformAccessAuthorization.PlatformAccessAttempt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Kuador access to a complex (ADR-011, D6 and D7). A request of a {@code PLATFORM} account on the host of a complex
 * runs only when {@link PlatformAccessAuthorization} allows it, which also audits it before the handler runs.
 * Otherwise it answers {@code 403 ACCESS_SESSION_INVALID}. Other requests are not touched.
 *
 * <p>It is a handler interceptor, not a filter, because the audit stores the route template of the chosen handler,
 * which only exists once the handler is mapped.
 */
final class PlatformAccessGuard implements HandlerInterceptor {

    /** Header with the id of the access session opened on the global host. */
    static final String ACCESS_SESSION_HEADER = "X-Platform-Access-Session";
    /** Route template audited when no handler pattern was matched. */
    static final String UNMATCHED_ROUTE = "[unmatched]";

    private final PlatformAccessAuthorization authorization;

    PlatformAccessGuard(PlatformAccessAuthorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<TenantId> tenant = TenantScope.current();
        Optional<AuthenticatedPrincipal> platformUser =
                CurrentPrincipal.current().filter(principal -> principal.is(AccountType.PLATFORM));
        if (tenant.isEmpty() || platformUser.isEmpty()) {
            return true;
        }
        if (!authorization.authorize(attemptOf(platformUser.get(), tenant.get(), request))) {
            throw new TenantRequestRejectedException(TenancyProblem.ACCESS_SESSION_INVALID);
        }
        return true;
    }

    private static PlatformAccessAttempt attemptOf(AuthenticatedPrincipal platformUser, TenantId tenant,
            HttpServletRequest request) {
        return new PlatformAccessAttempt(
                platformUser.accountId(),
                platformUser.isSuperAdmin(),
                tenant,
                Optional.ofNullable(request.getHeader(ACCESS_SESSION_HEADER)),
                request.getMethod(),
                routeTemplateOf(request));
    }

    private static String routeTemplateOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? UNMATCHED_ROUTE : pattern.toString();
    }
}
