package org.usf.inspect.server;

import java.time.Duration;

import org.usf.inspect.Retention;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;

public final class RetentionConfigDeserializer extends tools.jackson.databind.deser.std.StdDeserializer<Retention>  {

    RetentionConfigDeserializer() {
        super(Retention.class);
    }
    
    @Override
    public Retention deserialize(JsonParser jp, DeserializationContext ctxt) {
        JsonNode node = ctxt.readTree(jp);

        if (node != null && !node.isNull()) {
            if (node.isNumber() || node.isTextual()) {
                Duration retentionMaxAge = toDuration(node);
                return new Retention(retentionMaxAge, retentionMaxAge);
            } else if (node.isObject()) {
                // CAS NOMINAL
                Duration diagnostic = node.has("diagnostic") ? toDuration(node.get("diagnostic")) : null;
                Duration audit = node.has("audit") ? toDuration(node.get("audit")) : null;
                return new Retention(diagnostic, audit);
            } else {
                ctxt.reportInputMismatch(Retention.class, "Invalid Retention configuration: " + node.toString()); //throws exception
            }
        }
        return null;
    }

    private Duration toDuration(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            return Duration.ofSeconds(node.longValue());
        }

        if (node.isTextual()) {
            String value = node.asText();
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return Duration.parse(value);
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid duration value: " + value, e);
            }
        }

        return null;
    }
}