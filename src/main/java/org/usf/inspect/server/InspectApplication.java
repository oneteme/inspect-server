package org.usf.inspect.server;

import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.TraceDispatcherHub.createHub;
import static org.usf.inspect.server.JsonUtils.defaultMapper;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.ApplicationPropertiesProvider;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.core.TraceExporter;
import org.usf.inspect.server.config.ApplicationInspectPropertiesProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class InspectApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(InspectApplication.class, args);
	}

	@Bean
	@Primary
	ObjectMapper mapper(){
		return defaultMapper;
	}

	@Bean
	@ConfigurationProperties(prefix = "inspect.server")
	InspectServerConfiguration serverConfigurationProperties() {
		return new InspectServerConfiguration();
	}

	@Bean
	TraceDispatcherHub inspectServerContext(InspectServerConfiguration conf, TraceExporter agent, ObjectMapper mapper) {
		var ctx = (TraceDispatcherHub) createHub(conf, agent, mapper);
		ctx.setState(DISABLE); //until ready state
		return ctx;
	}
	
	@Bean
	ApplicationListener<ApplicationReadyEvent> enableDispatcherOnReady(@Qualifier("inspectServerContext") TraceDispatcherHub ctx){
		return e-> ctx.setState(ctx.getConfiguration().getScheduling().getState()); //wait for server startup before activate dispatcher
	}

    @Bean //used by inspect-core to get application properties and git info
    @Lazy //only if inspect-core is used
    public static ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) throws IOException {
        var props = new Properties();
        var resource = new ClassPathResource("git.properties");
        if(resource.exists()){
            props.load(resource.getInputStream());
        }
        return new ApplicationInspectPropertiesProvider(env, props);
    }
}