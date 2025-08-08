package org.usf.inspect.server;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.usf.inspect.core.SchedulingProperties;
import org.usf.inspect.core.TracingProperties;

@Setter
@Getter
@ToString
public final class InspectServerConfiguration {
    private SchedulingProperties scheduling = new SchedulingProperties();
    private TracingProperties tracing = new TracingProperties();
    private PartitionProperties partition = new PartitionProperties();
}
