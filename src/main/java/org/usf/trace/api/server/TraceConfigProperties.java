package org.usf.trace.api.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "trace")
@Getter
@Setter
public class TraceConfigProperties {
    private int period;
    private String timeUnit;
}
