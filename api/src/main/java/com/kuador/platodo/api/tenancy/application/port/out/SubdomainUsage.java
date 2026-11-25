package com.kuador.platodo.api.tenancy.application.port.out;

import java.util.Collection;
import java.util.Set;

import com.kuador.platodo.api.tenancy.domain.Subdomain;

/** Which subdomains complexes use or used: current and retired ones are never available again (ADR-012). */
public interface SubdomainUsage {

    /** The ones of {@code candidates} that a complex uses or used, in one lookup. */
    Set<Subdomain> usedAmong(Collection<Subdomain> candidates);
}
