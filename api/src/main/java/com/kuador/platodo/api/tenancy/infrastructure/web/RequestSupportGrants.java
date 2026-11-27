package com.kuador.platodo.api.tenancy.infrastructure.web;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.kuador.platodo.api.tenancy.application.port.in.SupportGrants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link SupportGrants} kept as an attribute of the current request: {@link SupportAccessGuard} records the grant once
 * the access is authorized and audited, and it ends with the request. Outside a web request nothing is granted.
 */
final class RequestSupportGrants implements SupportGrants {

    private static final String REQUEST_ATTRIBUTE = "com.kuador.platodo.api.tenancy.SupportGrant";

    /** Records that {@code platformUserId} may work on {@code tenant} during {@code request}. */
    static void grant(HttpServletRequest request, UUID platformUserId, TenantId tenant) {
        request.setAttribute(REQUEST_ATTRIBUTE, new Grant(platformUserId, tenant));
    }

    @Override
    public boolean isGrantedTo(UUID platformUserId) {
        Optional<TenantId> tenant = TenantScope.current();
        return tenant.isPresent() && currentGrant()
                .filter(grant -> grant.platformUserId().equals(platformUserId) && grant.tenant().equals(tenant.get()))
                .isPresent();
    }

    private static Optional<Grant> currentGrant() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .filter(Grant.class::isInstance)
                .map(Grant.class::cast);
    }

    private record Grant(UUID platformUserId, TenantId tenant) {

        private Grant {
            Objects.requireNonNull(platformUserId, "platformUserId");
            Objects.requireNonNull(tenant, "tenant");
        }
    }
}
