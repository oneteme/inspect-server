package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.EventTrace;

import java.time.Instant;

@Getter
@Setter
public class MachineResourceUsage implements EventTrace {
    private Instant instant;
    private int lowHeap;
    private int highHeap;
    private int lowMeta;
    private int highMeta;
    private String instanceId;
}
