package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartitionProperties {
    private boolean enabled = false;
    private SessionPartitionConfiguration session;
    private RequestPartitionConfiguration request;
}
