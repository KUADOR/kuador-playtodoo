package com.kuador.platodo.api.tenancy.application;

import java.util.List;
import java.util.Set;

import com.kuador.platodo.api.tenancy.application.port.out.SubdomainUsage;
import com.kuador.platodo.api.tenancy.domain.Subdomain;
import com.kuador.platodo.api.tenancy.domain.SubdomainCandidates;
import com.kuador.platodo.api.tenancy.domain.SubdomainRules;

/**
 * Suggests the first free subdomain for a commercial name (ADR-012). Candidates are checked a page at a time, one
 * lookup per page. The suggestion reserves nothing: registration validates it again.
 */
public final class SuggestSubdomain {

    /** Candidates checked per lookup; a name rarely needs more than the first page. */
    static final int PAGE_SIZE = 20;

    private final SubdomainRules rules;
    private final SubdomainUsage usage;

    SuggestSubdomain(SubdomainRules rules, SubdomainUsage usage) {
        this.rules = rules;
        this.usage = usage;
    }

    public Subdomain suggest(String displayName) {
        SubdomainCandidates candidates = SubdomainCandidates.from(displayName);
        for (int skipped = 0; ; skipped += PAGE_SIZE) {
            List<Subdomain> page = candidates.page(skipped, PAGE_SIZE);
            Set<Subdomain> used = usage.usedAmong(page);
            for (Subdomain candidate : page) {
                if (!used.contains(candidate) && !rules.isReserved(candidate)) {
                    return candidate;
                }
            }
        }
    }
}
