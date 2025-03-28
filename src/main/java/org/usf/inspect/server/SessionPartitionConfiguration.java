package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.usf.inspect.server.model.Partition;

@Setter
@Getter
@ToString
public class SessionPartitionConfiguration {

    private Partition http;
    private Partition main;
}
