package org.usf.inspect.server.retention;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public final class RetentionModels {

    private RetentionModels() {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
            @JsonSubTypes.Type(ModernRetention.class),
            @JsonSubTypes.Type(LegacyRetention.class)
    })
    public sealed interface RetentionConfig permits LegacyRetention, ModernRetention {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModernRetention(
            @JsonProperty("audit") Object audit,
            @JsonProperty("diagnostic") Object diagnostic
    ) implements RetentionConfig {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegacyRetention(
            @JsonProperty("retentionMaxAge") Object maxAge
    ) implements RetentionConfig {}
}