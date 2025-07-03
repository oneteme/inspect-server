package org.usf.inspect.server.model.lazy;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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
