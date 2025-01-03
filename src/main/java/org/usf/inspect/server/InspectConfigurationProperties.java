package org.usf.inspect.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@Setter
@Getter
@ToString
@ConfigurationProperties(prefix = "inspect.purge")
@RequiredArgsConstructor
public class InspectConfigurationProperties {
    private boolean enabled= true;
    private String schedule = "0 0 * * * *";
    private int depth = 90;
    private Map<String, Integer> env;
}
