package org.usf.inspect.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(InspectPartitionConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect.partition", name="enabled", havingValue = "true")
@Getter
@RequiredArgsConstructor
public class InspectPartitionConfiguration {
    private final InspectPartitionConfigurationProperties config;
}
