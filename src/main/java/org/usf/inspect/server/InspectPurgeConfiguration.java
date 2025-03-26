package org.usf.inspect.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@EnableConfigurationProperties(InspectPurgeConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect.purge", name="enabled", havingValue = "true")
@Getter
@RequiredArgsConstructor
public class InspectPurgeConfiguration {
    private final InspectPurgeConfigurationProperties config;
}
