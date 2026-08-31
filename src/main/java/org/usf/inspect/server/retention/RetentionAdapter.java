package org.usf.inspect.server.retention;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;

public final class RetentionAdapter {

    private final Duration defaultRetention;

    public RetentionAdapter(Duration defaultRetention) {
        this.defaultRetention = defaultRetention;
    }

    public Duration resolve(RetentionModels.RetentionConfig config, boolean diagnostic) {
        if (config == null) {
            return defaultRetention;
        }

        return switch (config) {
            case RetentionModels.ModernRetention modern -> {
                Object raw = diagnostic ? modern.diagnostic() : modern.audit();
                Duration d = asDuration(raw);
                yield d != null ? d : defaultRetention;
            }
            case RetentionModels.LegacyRetention legacy -> {
                Duration d = asDuration(legacy.maxAge());
                yield d != null ? d : defaultRetention;
            }
        };
    }

    private Duration asDuration(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Duration d) {
            return d;
        }
        if (raw instanceof Number n) {
            return Duration.ofSeconds(n.longValue());
        }
        if (raw instanceof String s) {
            String value = s.trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Duration.parse(value);
            } catch (DateTimeParseException ignored) {
                try {
                    return Duration.ofSeconds(new BigDecimal(value).longValue());
                } catch (NumberFormatException ignoredNumber) {
                    return null;
                }
            }
        }
        return null;
    }
}