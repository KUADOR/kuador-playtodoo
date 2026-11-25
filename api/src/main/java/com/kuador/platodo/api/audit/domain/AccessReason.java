package com.kuador.platodo.api.audit.domain;

/**
 * Why the Kuador owner opens an access session to a complex (ADR-003): mandatory, trimmed and at most
 * {@value #MAX_LENGTH} characters. It is free text, so it may hold personal data despite the guidance of the
 * backoffice; the purge of the complex anonymizes it.
 */
public record AccessReason(String value) {

    public static final int MAX_LENGTH = 500;

    /** What is left of every reason of a complex after its permanent deletion (ADR-013). */
    public static final AccessReason REDACTED = new AccessReason("[anonimizado por baja definitiva]");

    /**
     * @throws ReasonRequiredException if {@code value} is missing or blank
     * @throws ReasonTooLongException  if it is longer than {@value #MAX_LENGTH} characters once trimmed
     */
    public AccessReason {
        if (value == null || value.isBlank()) {
            throw new ReasonRequiredException();
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new ReasonTooLongException();
        }
    }
}
