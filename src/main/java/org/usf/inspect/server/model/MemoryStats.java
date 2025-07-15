package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonTypeName("--")
public class MemoryStats {
    private Instant instant;
    private int lowHeap;
    private int highHeap;
    private int lowMeta;
    private int highMeta;
    private String instanceId;
}
