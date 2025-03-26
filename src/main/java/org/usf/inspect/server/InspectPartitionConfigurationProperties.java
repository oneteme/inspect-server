package org.usf.inspect.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.temporal.ChronoUnit;
@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = "inspect.partition")
@RequiredArgsConstructor
public class InspectPartitionConfigurationProperties {
    private boolean enabled= true;
    private String schedule = "0 0 0 L * ?";
    private ChronoUnit frequency = ChronoUnit.MONTHS;
    private SessionPartitionConfiguration session = new SessionPartitionConfiguration();
    private RequestPartitionConfiguration request = new RequestPartitionConfiguration();
}
