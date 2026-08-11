package org.usf.inspect.server.validation;
import static org.usf.inspect.server.Utils.isUUID;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Defines reusable predicates for custom validation rules.
 */
public enum Condition {
    UUID(o -> isUUID(o.toString())),
    INSTANT(o -> isInstant(o.toString())),
    NOT_EMPTY(Condition::isNotEmpty);
    private final Predicate<Object> predicate;
    Condition(Predicate<Object> predicate) {
        this.predicate = predicate;
    }

    /**
     * Evaluates whether the supplied value matches this validation condition.
     *
     * @param value the value to evaluate
     * @return {@code true} if the value satisfies the condition, otherwise {@code false}
     */
    public boolean test(Object value) {
        return predicate.test(value);
    }

    private static boolean isInstant(String value) {
        try {
            return Instant.parse(value).isAfter(Instant.EPOCH);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isNotEmpty(Object value) {
        if (value instanceof Object[] array) {
            return array.length > 0;
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return value != null;
    }
}
