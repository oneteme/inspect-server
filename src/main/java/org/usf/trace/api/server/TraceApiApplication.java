package org.usf.trace.api.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.traceapi.core.RestSession;
import org.usf.traceapi.core.MainSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

@SpringBootApplication
@EnableTransactionManagement
public class TraceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceApiApplication.class, args);
	}

	@Bean
	@Primary
	public ObjectMapper mapper(){
		var mapper = json()
				.modules(new JavaTimeModule(), new ParameterNamesModule())
				.build(); //TODO ignore null & empty field configuration
		mapper.registerSubtypes(InstanceRestSession.class, InstanceMainSession.class);
		return mapper;
	}
}
