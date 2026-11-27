package com.kuador.platodo.api.bookings.infrastructure.identity;

import java.util.UUID;

import com.kuador.platodo.api.bookings.application.NotStaffOfTenantException;
import com.kuador.platodo.api.bookings.application.StaffNotAuthenticatedException;
import com.kuador.platodo.api.bookings.application.port.out.CurrentStaff;
import com.kuador.platodo.api.identity.application.port.in.AuthenticatedPrincipal;
import com.kuador.platodo.api.identity.application.port.in.CurrentPrincipal;
import com.kuador.platodo.api.identity.domain.AccountType;
import com.kuador.platodo.api.tenancy.application.port.in.SupportGrants;
import com.kuador.platodo.api.tenancy.application.port.in.TenantScope;

/**
 * The staff member of the request, from the access token Identity and Access accepted. The token is only accepted on
 * the host of its own complex; the tenant is checked again here so that a staff member never acts on another one.
 * Kuador support acts as staff only on a request Tenancy let through with a valid, audited support session
 * (ADR-020): it reuses the staff use cases instead of duplicating them, and its platform user id is what the bookings
 * keep as who created or cancelled them.
 */
public final class PrincipalCurrentStaff implements CurrentStaff {

    private final SupportGrants supportGrants;

    public PrincipalCurrentStaff(SupportGrants supportGrants) {
        this.supportGrants = supportGrants;
    }

    @Override
    public UUID require() {
        AuthenticatedPrincipal principal = CurrentPrincipal.current().orElseThrow(StaffNotAuthenticatedException::new);
        boolean staffOfThisTenant = principal.is(AccountType.STAFF)
                && principal.staffTenant().equals(TenantScope.current());
        boolean supportOnThisTenant = principal.is(AccountType.PLATFORM)
                && supportGrants.isGrantedTo(principal.accountId());
        if (!staffOfThisTenant && !supportOnThisTenant) {
            throw new NotStaffOfTenantException();
        }
        return principal.accountId();
    }
}
