package com.kuador.platodo.api.subscriptions.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A valid request to register a complex: commercial name, subdomain, owner e-mail and the plan to assign. The
 * subdomain is checked by Tenancy (ADR-012) and the plan by its publication, not here.
 *
 * @param displayName trimmed, 1 to {@value #MAX_DISPLAY_NAME_LENGTH} characters
 * @param ownerEmail  trimmed and in lower case
 */
public record ComplexRegistration(String displayName, String subdomain, String ownerEmail, UUID planId) {

    /** Longest commercial name, as Tenancy stores it. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 120;
    /** Longest e-mail address (RFC 5321 path limit). */
    public static final int MAX_EMAIL_LENGTH = 254;

    /** One local part, one {@code @} and a domain with at least one dot; no spaces. Deliverability is not checked. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public ComplexRegistration {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(subdomain, "subdomain");
        Objects.requireNonNull(ownerEmail, "ownerEmail");
        Objects.requireNonNull(planId, "planId");
    }

    /**
     * Validates and normalizes the fields of a registration request.
     *
     * @throws InvalidComplexRegistrationException with every invalid field
     */
    public static ComplexRegistration of(String displayName, String subdomain, String ownerEmail, UUID planId) {
        List<FieldError> errors = new ArrayList<>();
        String name = displayName == null ? "" : displayName.strip();
        if (name.isEmpty()) {
            errors.add(new FieldError("displayName", FieldError.Code.REQUIRED));
        } else if (name.length() > MAX_DISPLAY_NAME_LENGTH) {
            errors.add(new FieldError("displayName", FieldError.Code.TOO_LONG));
        }
        String email = ownerEmail == null ? "" : ownerEmail.strip().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            errors.add(new FieldError("ownerEmail", FieldError.Code.REQUIRED));
        } else if (email.length() > MAX_EMAIL_LENGTH) {
            errors.add(new FieldError("ownerEmail", FieldError.Code.TOO_LONG));
        } else if (!EMAIL.matcher(email).matches()) {
            errors.add(new FieldError("ownerEmail", FieldError.Code.INVALID_FORMAT));
        }
        if (!errors.isEmpty()) {
            throw new InvalidComplexRegistrationException(errors);
        }
        return new ComplexRegistration(name, subdomain, email, planId);
    }
}
