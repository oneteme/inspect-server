package org.usf.inspect.server.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class InstanceTrace {
    private Integer pending;
    private int attemps;
    private int sessionLength;
    private Instant instant;
    private String  instanceId;

}
