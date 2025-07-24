package org.usf.inspect.server.model;

import java.time.Instant;

public interface Metric extends InstanceEventTrace {
    Instant getStart();

    Instant getEnd();
}
