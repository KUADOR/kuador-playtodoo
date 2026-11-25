package com.kuador.platodo.api.identity.domain;

import java.util.Locale;
import java.util.Objects;

/** Login e-mail in the normalized form accounts are stored with: trimmed and lower case. */
public record EmailAddress(String value) {

    public EmailAddress {
        Objects.requireNonNull(value, "value");
    }

    public static EmailAddress normalize(String email) {
        return new EmailAddress(email.strip().toLowerCase(Locale.ROOT));
    }
}
