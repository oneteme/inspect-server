package org.usf.inspect.server.validation;
import org.usf.inspect.server.Utils;

import java.util.function.Predicate;
public enum Condition {
    UUID(Utils::isUUID),
    NOT_BLANK(s -> s != null && !s.isBlank());
    private final Predicate<String> predicate;
    Condition(Predicate<String> predicate) {
        this.predicate = predicate;
    }
    public boolean test(String value) {
        return predicate.test(value);
    }
}
