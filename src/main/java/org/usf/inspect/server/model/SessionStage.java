package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SessionStage {
    private String user;
    private Instant start;
    private Instant end;
    private String threadName;
}
