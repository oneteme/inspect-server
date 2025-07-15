package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonTypeName("--")
public class LogEntry {

    private final Instant instant;
    private final Level level;
    private final String message;
    private String sessionId; //nullable

    enum Level {
        INFO, WARN, ERROR;
    }
}
