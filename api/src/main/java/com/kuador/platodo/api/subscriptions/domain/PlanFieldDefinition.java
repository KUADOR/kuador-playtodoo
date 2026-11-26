package com.kuador.platodo.api.subscriptions.domain;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Definition of a field a plan can have (ADR-018). Basic fields are read-only and never retired. A custom field keeps
 * its {@code key} and {@code type} forever; its label and whether it is required can change, and it can be retired.
 * Every effective change raises {@code version} by one.
 */
public record PlanFieldDefinition(UUID id, String key, String label, PlanFieldType type, boolean required,
        PlanFieldOrigin origin, Optional<Instant> retiredAt, long version) {

    public static final int MAX_LABEL_LENGTH = 80;

    private static final String LABEL = "label";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_KEY_CHARACTERS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final String HYPHEN = "-";

    public PlanFieldDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(retiredAt, "retiredAt");
    }

    /**
     * A new custom field whose key is derived from its label.
     *
     * @throws InvalidPlanFieldsException if the label is blank, too long or has no letter or digit
     */
    public static PlanFieldDefinition custom(UUID id, String label, PlanFieldType type, boolean required) {
        String validLabel = validLabel(label);
        String key = keyOf(validLabel);
        if (key.isEmpty()) {
            throw new InvalidPlanFieldsException(List.of(new FieldError(LABEL, FieldError.Code.INVALID_FORMAT)));
        }
        return new PlanFieldDefinition(id, key, validLabel, type, required, PlanFieldOrigin.CUSTOM,
                Optional.empty(), 0);
    }

    /** Lower case, accents removed, every run of other characters replaced by one hyphen, no hyphens at the ends. */
    static String keyOf(String label) {
        String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(label, Normalizer.Form.NFD)).replaceAll("");
        String hyphenated = NOT_KEY_CHARACTERS.matcher(withoutAccents.toLowerCase(Locale.ROOT)).replaceAll(HYPHEN);
        return EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
    }

    /**
     * The same field with another label and required flag; itself when nothing changes.
     *
     * @throws BasicFieldReadOnlyException for a basic field
     * @throws InvalidPlanFieldsException  if the label is blank or too long
     */
    public PlanFieldDefinition relabel(String newLabel, boolean newRequired) {
        requireCustom();
        String validLabel = validLabel(newLabel);
        if (validLabel.equals(label) && newRequired == required) {
            return this;
        }
        return new PlanFieldDefinition(id, key, validLabel, type, newRequired, origin, retiredAt, version + 1);
    }

    /**
     * The field retired at {@code now}; itself when it already was.
     *
     * @throws BasicFieldReadOnlyException for a basic field
     */
    public PlanFieldDefinition retire(Instant now) {
        requireCustom();
        if (isRetired()) {
            return this;
        }
        return new PlanFieldDefinition(id, key, label, type, required, origin, Optional.of(now), version + 1);
    }

    public boolean isRetired() {
        return retiredAt.isPresent();
    }

    public boolean isCustom() {
        return origin == PlanFieldOrigin.CUSTOM;
    }

    /** A custom field that is not retired: plans may set it. */
    public boolean isOffered() {
        return isCustom() && !isRetired();
    }

    private void requireCustom() {
        if (!isCustom()) {
            throw new BasicFieldReadOnlyException(key);
        }
    }

    private static String validLabel(String label) {
        String trimmed = label == null ? "" : label.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidPlanFieldsException(List.of(new FieldError(LABEL, FieldError.Code.REQUIRED)));
        }
        if (trimmed.length() > MAX_LABEL_LENGTH) {
            throw new InvalidPlanFieldsException(List.of(new FieldError(LABEL, FieldError.Code.TOO_LONG)));
        }
        return trimmed;
    }
}
