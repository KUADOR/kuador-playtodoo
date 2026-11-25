package com.kuador.platodo.api.tenancy.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Subdomain candidates suggested from the commercial name of a complex (ADR-012): lower case, accents removed and
 * every run of other characters replaced by one hyphen; then {@code -2}, {@code -3}, and so on. Every candidate is
 * well formed. Whether it is free is decided by the caller.
 */
public final class SubdomainCandidates {

    /**
     * Used when the name has no letter or digit at all, and appended to names shorter than the minimum length.
     * It is user-facing: it becomes part of the address of the complex.
     */
    static final String FALLBACK = "complejo";

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;
    private static final int FIRST_NUMBER = 2;
    private static final String HYPHEN = "-";
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

    private final String base;

    private SubdomainCandidates(String base) {
        this.base = base;
    }

    public static SubdomainCandidates from(String displayName) {
        return new SubdomainCandidates(baseOf(displayName));
    }

    /** The {@code size} candidates that follow the first {@code skipped} ones, in order. */
    public List<Subdomain> page(int skipped, int size) {
        return IntStream.range(skipped, skipped + size).mapToObj(this::candidate).toList();
    }

    /** Candidate number {@code index}: the base itself first, then the base with {@code -2}, {@code -3}... */
    private Subdomain candidate(int index) {
        if (index == 0) {
            return new Subdomain(base);
        }
        String suffix = HYPHEN + (index - 1 + FIRST_NUMBER);
        return new Subdomain(trimmed(base, MAX_LENGTH - suffix.length()) + suffix);
    }

    private static String baseOf(String displayName) {
        String withoutAccents = COMBINING_MARKS.matcher(Normalizer.normalize(displayName, Normalizer.Form.NFD))
                .replaceAll("");
        String hyphenated = NOT_ALPHANUMERIC.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(HYPHEN);
        String base = trimmed(hyphenated, MAX_LENGTH);
        if (base.isEmpty()) {
            return FALLBACK;
        }
        return base.length() < MIN_LENGTH ? base + HYPHEN + FALLBACK : base;
    }

    /** At most {@code maxLength} characters, without hyphens at the ends. */
    private static String trimmed(String value, int maxLength) {
        String withoutEdges = EDGE_HYPHENS.matcher(value).replaceAll("");
        String cut = withoutEdges.length() > maxLength ? withoutEdges.substring(0, maxLength) : withoutEdges;
        return EDGE_HYPHENS.matcher(cut).replaceAll("");
    }
}
