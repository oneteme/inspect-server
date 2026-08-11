package org.usf.inspect.server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

/**
 * Stores trace counters and metadata for an inspected instance.
 */
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

    /**
     * Increases the number of pending traces by the given amount.
     *
     * @param delta the amount to add to the pending trace count
     */
    public void addPending(int delta) {
        pending += delta;
    }

    /**
     * Decreases the number of pending traces by the given amount.
     *
     * @param delta the amount to subtract from the pending trace count
     */
    public void removePending(int delta) {
        pending -= delta;
    }

    /**
     * Increases the trace count by the given amount.
     *
     * @param delta the amount to add to the trace count
     */
    public void addTraceCount(int delta) {
        traceCount += delta;
    }
}
