package org.usf.inspect.server.model;

import java.time.Instant;

public interface Metric {
    Instant getStart();

    Instant getEnd();
}
