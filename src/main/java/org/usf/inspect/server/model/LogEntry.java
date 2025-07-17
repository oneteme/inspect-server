package org.usf.inspect.server.model;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class LogEntry implements Traceable {

    private Instant instant;
    private Level level;
    private String message;
    private String sessionId; //nullable
    private String instanceId;

    enum Level {
        INFO, WARN, ERROR;
    }
}
