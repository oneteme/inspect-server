package org.usf.inspect.server;

import org.usf.inspect.server.model.Partition;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class SessionPartitionConfiguration {

    private Partition http;
    private Partition main;
}
