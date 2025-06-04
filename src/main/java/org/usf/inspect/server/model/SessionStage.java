package org.usf.inspect.server.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionStage {
        private String user;
        private Instant start;
        private Instant end;
        private String threadName;

        private Long idRequest;
        private String cdSession;
}
