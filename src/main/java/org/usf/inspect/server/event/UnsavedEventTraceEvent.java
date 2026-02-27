package org.usf.inspect.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UnsavedEventTraceEvent extends ApplicationEvent {
    Object object;

    public UnsavedEventTraceEvent(Object source, Object object) {
        super(source);
        this.object = object;
    }
}
