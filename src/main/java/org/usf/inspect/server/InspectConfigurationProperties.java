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
    private boolean enabled= false;
    private String schedule = "* * * * * ?";
    private int depth = 90;
    private Map<String, Integer> env;
}
