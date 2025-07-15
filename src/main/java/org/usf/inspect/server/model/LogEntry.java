package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonTypeName("--")
public class LogEntry {

    private Instant instant;
    private Level level;
    private String message;
    private String sessionId; //nullable
    private String instanceId;

    enum Level {
        INFO, WARN, ERROR;
    }
}
