package com.kuador.platodo.api.tenancy.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which subdomains a complex may take (ADR-012): the format of {@link Subdomain} and a reserved list kept in
 * configuration. Whether a subdomain is already used, or was used, is a question for the registry, not for the rules.
 */
public final class SubdomainRules {

    /** Why a candidate can never be the subdomain of a complex. */
    public enum Rejection {
        /** It breaks the format rules. */
        INVALID,
        /** It is in the reserved list. */
        RESERVED
    }

    private final Set<String> reserved;

    /** {@code reserved} names are compared in lower case. */
    public SubdomainRules(Collection<String> reserved) {
        this.reserved = reserved.stream()
                .map(String::strip)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Why {@code candidate} (compared in lower case) cannot be a subdomain, or empty when the rules allow it. */
    public Optional<Rejection> rejectionOf(String candidate) {
        Optional<Subdomain> subdomain = Subdomain.parse(candidate);
        if (subdomain.isEmpty()) {
            return Optional.of(Rejection.INVALID);
        }
        return isReserved(subdomain.get()) ? Optional.of(Rejection.RESERVED) : Optional.empty();
    }

    /**
     * The subdomain written in {@code candidate}, normalized to lower case.
     *
     * @throws SubdomainRejectedException if the rules do not allow it
     */
    public Subdomain accept(String candidate) {
        Optional<Rejection> rejection = rejectionOf(candidate);
        if (rejection.isPresent()) {
            throw new SubdomainRejectedException(rejection.get());
        }
        return Subdomain.parse(candidate).orElseThrow();
    }

    public boolean isReserved(Subdomain subdomain) {
        return reserved.contains(subdomain.value());
    }
}
