package org.usf.inspect.server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class InstanceTrace implements EventTrace {

    private final Integer attempts;
    private final String fileName;
    private final Instant instant;
    private final String instanceId;
    private int traceCount = 0;
    private int pending = 0;

    public void addPending(int delta) {
        pending += delta;
    }

    public void removePending(int delta) {
        pending -= delta;
    }

    public void addTraceCount(int delta) {
        traceCount += delta;
    }
}
