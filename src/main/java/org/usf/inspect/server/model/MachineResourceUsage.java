package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class MachineResourceUsage implements Traceable {
    private Instant instant;
    private int lowHeap;
    private int highHeap;
    private int lowMeta;
    private int highMeta;
    private String instanceId;
}
