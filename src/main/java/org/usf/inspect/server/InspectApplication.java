package org.usf.inspect.server;

import static org.usf.inspect.server.JsonUtils.defaultMapper;

import java.io.IOException;
import java.util.Properties;

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
import org.usf.inspect.core.TracePublisher;
import org.usf.inspect.server.config.ApplicationInspectPropertiesProvider;
import org.usf.inspect.server.service.TraceBatchDispatcherHub;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	TraceBatchDispatcherHub inspectServerContext() {
		return new TraceBatchDispatcherHub(); //disable by default
	}
	
	@Bean
	ApplicationListener<ApplicationReadyEvent> enableDispatcherOnReady(TraceDispatcherHub ctx, InspectServerConfiguration conf, TracePublisher agent) {
		return e-> ctx.configure(conf, agent); //wait for server startup before start dispatcher
	}

    @Bean //used by inspect-core to get application properties and git info
    @Lazy //only if inspect-core is used
    static ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) {
        var rsr = new ClassPathResource("git.properties");
        var prp = new Properties();
        if(rsr.exists()){
        	try {
        		prp.load(rsr.getInputStream());
        	}
        	catch (IOException e) {
        		log.warn("Failed to load git.properties", e);
			}
        }
        return new ApplicationInspectPropertiesProvider(env, prp);
    }
}