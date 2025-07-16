package org.usf.inspect.server.model;

import java.time.Instant;

public interface  Metric extends Traceable {
    Instant getStart();

    Instant getEnd();
}
