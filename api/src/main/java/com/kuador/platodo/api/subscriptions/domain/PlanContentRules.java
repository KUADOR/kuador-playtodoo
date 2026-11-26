package com.kuador.platodo.api.subscriptions.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks the fields of a plan (ADR-018) and reports every invalid one at once:
 * <ul>
 *   <li>{@code name}: required, at most {@value #MAX_NAME_LENGTH} characters.</li>
 *   <li>{@code price}: decimal text with up to 15 integer digits and 4 decimals, never negative.</li>
 *   <li>{@code currency} and {@code periodicity}: one of the {@link PlanOptions}.</li>
 *   <li>{@code courtLimit} and {@code userLimit}: not negative.</li>
 *   <li>Custom fields: the key of an offered field and a value of its type. A text value is at most
 *       {@value #MAX_TEXT_VALUE_LENGTH} characters; a blank text or a {@code null} clears the value.</li>
 *   <li>Retired fields are no longer set, but their stored values are kept. Sending one back with its stored value
 *       (as a client that read the plan does) is accepted; any other value is not allowed.</li>
 * </ul>
 * Whether the plan is complete enough to be published is decided by {@link Plan}, not here.
 */
public final class PlanContentRules {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_TEXT_VALUE_LENGTH = 500;

    private static final Pattern MONEY = Pattern.compile("^\\d{1,15}(\\.\\d{1,4})?$");
    private static final String NAME = "name";
    private static final String PRICE = "price";
    private static final String CURRENCY = "currency";
    private static final String PERIODICITY = "periodicity";
    private static final String COURT_LIMIT = "courtLimit";
    private static final String USER_LIMIT = "userLimit";

    private final PlanOptions options;
    private final Map<String, PlanFieldDefinition> fieldsByKey;

    public PlanContentRules(PlanOptions options, List<PlanFieldDefinition> fields) {
        this.options = options;
        this.fieldsByKey = fields.stream().collect(Collectors.toMap(PlanFieldDefinition::key, Function.identity()));
    }

    /** Fields of a new plan. */
    public PlanContent check(PlanDraft draft) {
        return check(draft, Map.of());
    }

    /**
     * Fields that replace those of an existing plan, whose stored custom values are {@code storedCustomFields}.
     *
     * @throws InvalidPlanFieldsException with every invalid field
     */
    public PlanContent check(PlanDraft draft, Map<String, Object> storedCustomFields) {
        List<FieldError> errors = new ArrayList<>();
        String name = checkName(draft.name(), errors);
        BigDecimal price = checkPrice(draft.price(), errors);
        checkAllowed(draft.currency(), options::allowsCurrency, CURRENCY, errors);
        checkAllowed(draft.periodicity(), options::allowsPeriodicity, PERIODICITY, errors);
        checkNotNegative(draft.courtLimit(), COURT_LIMIT, errors);
        checkNotNegative(draft.userLimit(), USER_LIMIT, errors);
        Map<String, Object> customFields = checkCustomFields(draft.customFields(), storedCustomFields, errors);
        if (!errors.isEmpty()) {
            throw new InvalidPlanFieldsException(errors);
        }
        return new PlanContent(name, price, draft.currency(), draft.periodicity(), draft.courtLimit(),
                draft.userLimit(), customFields);
    }

    private static String checkName(String name, List<FieldError> errors) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty()) {
            errors.add(new FieldError(NAME, FieldError.Code.REQUIRED));
        } else if (trimmed.length() > MAX_NAME_LENGTH) {
            errors.add(new FieldError(NAME, FieldError.Code.TOO_LONG));
        }
        return trimmed;
    }

    private static BigDecimal checkPrice(String price, List<FieldError> errors) {
        if (price == null) {
            return null;
        }
        if (!MONEY.matcher(price).matches()) {
            errors.add(new FieldError(PRICE, FieldError.Code.INVALID_FORMAT));
            return null;
        }
        return new BigDecimal(price);
    }

    private static void checkAllowed(String value, Predicate<String> allowed, String field,
            List<FieldError> errors) {
        if (value != null && !allowed.test(value)) {
            errors.add(new FieldError(field, FieldError.Code.NOT_ALLOWED));
        }
    }

    private static void checkNotNegative(Integer value, String field, List<FieldError> errors) {
        if (value != null && value < 0) {
            errors.add(new FieldError(field, FieldError.Code.NEGATIVE));
        }
    }

    private Map<String, Object> checkCustomFields(Map<String, Object> requested, Map<String, Object> stored,
            List<FieldError> errors) {
        Map<String, Object> accepted = new HashMap<>(retiredValues(stored));
        requested.forEach((key, value) -> {
            PlanFieldDefinition field = fieldsByKey.get(key);
            if (field == null || !field.isCustom()) {
                errors.add(FieldError.ofCustomField(key, FieldError.Code.UNKNOWN_FIELD));
            } else if (field.isRetired()) {
                checkRetiredValue(key, value, stored.get(key), errors);
            } else if (!isCleared(value)) {
                checkOfferedValue(field, value, errors).ifPresent(valid -> accepted.put(key, valid));
            }
        });
        return accepted;
    }

    private Map<String, Object> retiredValues(Map<String, Object> stored) {
        return stored.entrySet().stream()
                .filter(entry -> isRetired(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isRetired(String key) {
        PlanFieldDefinition field = fieldsByKey.get(key);
        return field != null && field.isRetired();
    }

    private static void checkRetiredValue(String key, Object value, Object storedValue, List<FieldError> errors) {
        boolean unchanged = isCleared(value) ? storedValue == null : sameValue(value, storedValue);
        if (!unchanged) {
            errors.add(FieldError.ofCustomField(key, FieldError.Code.NOT_ALLOWED));
        }
    }

    private static Optional<Object> checkOfferedValue(PlanFieldDefinition field, Object value,
            List<FieldError> errors) {
        if (!field.type().accepts(value)) {
            errors.add(FieldError.ofCustomField(field.key(), FieldError.Code.WRONG_TYPE));
            return Optional.empty();
        }
        if (value instanceof String text && text.strip().length() > MAX_TEXT_VALUE_LENGTH) {
            errors.add(FieldError.ofCustomField(field.key(), FieldError.Code.TOO_LONG));
            return Optional.empty();
        }
        return Optional.of(value instanceof String text ? text.strip() : value);
    }

    private static boolean isCleared(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }

    private static boolean sameValue(Object value, Object storedValue) {
        if (value instanceof BigDecimal number && storedValue instanceof BigDecimal storedNumber) {
            return number.compareTo(storedNumber) == 0;
        }
        return Objects.equals(value, storedValue);
    }
}
