package org.usf.inspect.server.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractRequest implements Metric {
        private String user;
        private Instant start;
        private Instant end;
        private String threadName;

        private String id;
        private String sessionId;
        private String instanceId;

        private boolean isCompleted;
}
