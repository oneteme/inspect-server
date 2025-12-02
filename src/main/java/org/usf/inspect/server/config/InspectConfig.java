package org.usf.inspect.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.usf.inspect.core.ApplicationPropertiesProvider;

import java.io.IOException;
import java.util.Properties;


@Configuration
public class InspectConfig {

    @Bean
    public static ApplicationPropertiesProvider springProperties(Environment env) throws IOException {
        var props = new Properties();
        var resource = new ClassPathResource("git.properties");
        if(resource.exists()){
            props.load(resource.getInputStream());
        }
        return new ApplicationInspectPropertiesProvider(env, props);
    }
}
