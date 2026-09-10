package org.usf.inspect.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.usf.inspect.core.Retention;

import java.io.IOException;
import java.time.Duration;

public final class RetentionConfigDeserializer extends StdDeserializer<Retention>  {

    RetentionConfigDeserializer() {
        super(Retention.class);
    }

    @Override
    public Retention deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

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
                throw new JsonMappingException(jp, "Invalid Retention configuration: " + node.toString());
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