package org.usf.inspect.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.BasicDispatchState.DISABLE;
import static org.usf.inspect.core.InspectContext.coreModule;

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
				.modules(new JavaTimeModule(), new ParameterNamesModule(), coreModule().registerSubtypes(new NamedType(InstanceTrace.class, "inst-trc"), new NamedType(InstanceEnvironmentUpdate.class, "inst-updt")))
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
	@Primary
	@ConfigurationProperties(prefix = "inspect.server")
	InspectServerConfiguration serverConfigurationProperties() {
		return new InspectServerConfiguration();
	}

	@Bean
	EventTraceScheduledDispatcher dispatcher(InspectServerConfiguration conf, DispatcherAgent agent, ObjectMapper mapper) {
		var hooks = new ArrayList<DispatchHook>();
		if(conf.getTracing().getDump().isEnabled()) {
			hooks.add(new EventTraceDumper(conf.getTracing().getDump().getLocation(), mapper));
		}
		var dspt = new EventTraceScheduledDispatcher(conf.getTracing(), conf.getScheduling(), agent, hooks);
		dspt.setState(DISABLE); //until ready state
		return dspt;
	}
	
	@Bean
	ApplicationListener<ApplicationReadyEvent> enableDispatcherOnReady(EventTraceScheduledDispatcher dispatcher, InspectServerConfiguration conf){
		return e-> dispatcher.setState(conf.getScheduling().getState()); //wait for server startup before activate dispatcher
	}
}
