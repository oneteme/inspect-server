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

        private String idRequest;
        private String cdSession;
        private String instanceId;
        private boolean isCompleted;
}
