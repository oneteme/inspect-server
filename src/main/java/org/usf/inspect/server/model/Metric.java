package org.usf.inspect.server.model;

import org.usf.inspect.core.EventTrace;

import java.time.Instant;

public interface  Metric extends EventTrace {
    Instant getStart();

    Instant getEnd();
}
