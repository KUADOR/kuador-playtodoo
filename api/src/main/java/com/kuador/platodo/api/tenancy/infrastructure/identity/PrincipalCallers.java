package com.kuador.platodo.api.tenancy.infrastructure.identity;

import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.CallerNotAllowedException;
import com.kuador.platodo.api.tenancy.application.CallerNotAuthenticatedException;
import com.kuador.platodo.api.tenancy.application.port.in.TenantId;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;
import com.kuador.platodo.api.tenancy.application.port.out.Callers;

/**
 * The caller of a Tenancy operation, from the principal Identity and Access accepted for the request. The staff
 * token is only accepted on the host of its own complex; the tenant is checked again here so that an owner never
 * acts on another complex.
 */
public final class PrincipalCallers implements Callers {

    static final String COMPLEX_OWNER = "OWNER";

    @Override
    public void requirePlatformOwner() {
        AuthenticatedPrincipal principal = authenticated();
        if (!principal.isSuperAdmin()) {
            throw new CallerNotAllowedException();
        }
    }

    @Override
    public TenantId requireComplexOwner() {
        AuthenticatedPrincipal principal = authenticated();
        TenantId hostTenant = TenantScope.require();
        boolean ownerOfThisComplex = principal.is(AccountType.STAFF)
                && COMPLEX_OWNER.equals(principal.role())
                && principal.staffTenant().filter(hostTenant::equals).isPresent();
        if (!ownerOfThisComplex) {
            throw new CallerNotAllowedException();
        }
        return hostTenant;
    }

    private static AuthenticatedPrincipal authenticated() {
        return CurrentPrincipal.current().orElseThrow(CallerNotAuthenticatedException::new);
    }
}
