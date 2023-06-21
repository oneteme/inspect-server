package org.usf.trace.api.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "trace")
public class TraceConfigProperties {
    private int period;
    private String timeUnit;
}
