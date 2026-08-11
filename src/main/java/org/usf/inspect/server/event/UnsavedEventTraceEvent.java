package org.usf.inspect.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.usf.inspect.core.EventTrace;

/**
 * Publishes an event trace that could not be persisted.
 */
@Getter
@SuppressWarnings("serial")
public class UnsavedEventTraceEvent extends ApplicationEvent {
    
	private final boolean retry;
	private final EventTrace trace;

    /**
     * Creates a new event for an unsaved trace.
     *
     * @param source the object on which the event initially occurred
     * @param trace the event trace that could not be saved
     * @param retry whether the trace persistence should be retried
     */
    public UnsavedEventTraceEvent(Object source, EventTrace trace, boolean retry) {
        super(source);
        this.trace = trace;
        this.retry = retry;
    }
}
