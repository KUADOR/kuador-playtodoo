package com.kuador.platodo.api.tenancy.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A well-formed subdomain of a sports complex (ADR-012): lower case {@code a-z}, {@code 0-9} and hyphen, 3 to 63
 * characters, no leading or trailing hyphen and no double hyphen. The double-hyphen rule also excludes the
 * {@code xn--} prefix of internationalized names. Reserved names are well formed; they simply never belong to a
 * complex.
 */
public record Subdomain(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");
    private static final String DOUBLE_HYPHEN = "--";

    public Subdomain {
        Objects.requireNonNull(value, "value");
        if (!isWellFormed(value)) {
            throw new IllegalArgumentException("Not a well-formed subdomain");
        }
    }

    /** The subdomain written in {@code candidate}, compared in lower case; empty when it breaks the format. */
    public static Optional<Subdomain> parse(String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        String normalized = candidate.toLowerCase(Locale.ROOT);
        return isWellFormed(normalized) ? Optional.of(new Subdomain(normalized)) : Optional.empty();
    }

    private static boolean isWellFormed(String value) {
        return FORMAT.matcher(value).matches() && !value.contains(DOUBLE_HYPHEN);
    }
}
