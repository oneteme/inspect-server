package org.usf.inspect.server.retention;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class RetentionConfigDeserializer extends JsonDeserializer<RetentionModels.RetentionConfig> {

    @Override
    public RetentionModels.RetentionConfig deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        if (node == null || node.isNull()) {
            return null;
        }

        if (node.has("tracing") && node.get("tracing").has("remote")) {
            node = node.get("tracing").get("remote");
        }

        if (node.has("diagnostic") || node.has("audit")) {
            return new RetentionModels.ModernRetention(
                    node.has("audit") ? node.get("audit").deepCopy() : null,
                    node.has("diagnostic") ? node.get("diagnostic").deepCopy() : null
            );
        }

        if (node.has("retentionMaxAge")) {
            return new RetentionModels.LegacyRetention(
                    node.has("retentionMaxAge") ? node.get("retentionMaxAge").deepCopy() : null
            );
        }

        return null;
    }
}