package org.usf.inspect.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.*;
import org.usf.inspect.server.config.ApplicationInspectPropertiesProvider;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.InspectConfiguration.coreModule;
import static org.usf.inspect.core.TraceDispatcherHub.createHub;

import java.io.IOException;
import java.util.Properties;

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
		var mapper = json()
				.modules(new JavaTimeModule(), new ParameterNamesModule(), coreModule().registerSubtypes(
						new NamedType(InstanceTrace.class, "inst-trc"), 
						new NamedType(InstanceEnvironmentUpdate.class, "inst-updt")))
				.build()
			    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // !null & !empty
		mapper.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true);
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(
				new NamedType(MainSessionWrapper.class, "main"), 
				new NamedType(RestSessionWrapper.class, "rest"));
		return mapper;
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
    public static ApplicationPropertiesProvider applicationPropertiesProvider(Environment env) throws IOException {
        var props = new Properties();
        var resource = new ClassPathResource("git.properties");
        if(resource.exists()){
            props.load(resource.getInputStream());
        }
        return new ApplicationInspectPropertiesProvider(env, props);
    }
}
