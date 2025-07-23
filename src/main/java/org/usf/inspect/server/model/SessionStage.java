package org.usf.inspect.server.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionStage implements Metric {
        private String user;
        private Instant start;
        private Instant end;
        private String threadName;

        @Deprecated(since = "v1.1", forRemoval = true)
        private String idRequest;
        @Deprecated(since = "v1.1", forRemoval = true)
        private String cdSession;
        private String instanceId;

        private String sessionId;
        private String id;à

        private boolean isCompleted;
}
