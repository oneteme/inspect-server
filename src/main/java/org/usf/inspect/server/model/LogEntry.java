package org.usf.inspect.server.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class LogEntry implements EventTrace {

    private Instant instant;
    private Level level;
    private String message;
    private String sessionId; //nullable
    private String instanceId;

    public enum Level {
        INFO, WARN, ERROR;
    }
}
