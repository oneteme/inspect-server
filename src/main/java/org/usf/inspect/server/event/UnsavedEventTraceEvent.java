package org.usf.inspect.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.usf.inspect.core.EventTrace;

@Getter
@SuppressWarnings("serial")
public class UnsavedEventTraceEvent extends ApplicationEvent {
    
	private final boolean retry;
	private final EventTrace trace;

    public UnsavedEventTraceEvent(Object source, EventTrace trace, boolean retry) {
        super(source);
        this.trace = trace;
        this.retry = retry;
    }
}
