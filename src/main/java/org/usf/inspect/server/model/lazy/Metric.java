package org.usf.inspect.server.model.lazy;

import java.time.Instant;

public interface Metric {
    Instant getStart();

    Instant getEnd();
}
