package com.kuador.platodo.api.tenancy.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;

/** Whether a subdomain can be used, checking format, reserved list and current or retired use, in that order. */
public final class CheckSubdomainAvailability {

    /** Why a subdomain is not available; {@code TAKEN} covers retired subdomains too. */
    public enum Reason { INVALID, RESERVED, TAKEN }

    /** @param reason null when available */
    public record Availability(String subdomain, Reason reason) {

        public boolean available() {
            return reason == null;
        }
    }

    private final SubdomainRules rules;
    private final SubdomainUsage usage;

    CheckSubdomainAvailability(SubdomainRules rules, SubdomainUsage usage) {
        this.rules = rules;
        this.usage = usage;
    }

    public Availability check(String candidate) {
        String normalized = candidate.toLowerCase(Locale.ROOT);
        Optional<SubdomainRules.Rejection> rejection = rules.rejectionOf(normalized);
        if (rejection.isPresent()) {
            return new Availability(normalized, Reason.valueOf(rejection.get().name()));
        }
        Subdomain subdomain = new Subdomain(normalized);
        boolean taken = !usage.usedAmong(List.of(subdomain)).isEmpty();
        return new Availability(normalized, taken ? Reason.TAKEN : null);
    }
}
