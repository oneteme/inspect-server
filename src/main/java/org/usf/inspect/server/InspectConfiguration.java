package org.usf.inspect.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(InspectConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect.purge", name="enabled", havingValue = "true")
@Getter
@RequiredArgsConstructor
@ToString
public class InspectConfiguration {


    private final InspectConfigurationProperties config;


}
