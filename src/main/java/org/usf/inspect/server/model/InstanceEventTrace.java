package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

@Getter
@Setter
public abstract class InstanceEventTrace implements EventTrace {
    private String instanceId;
}
