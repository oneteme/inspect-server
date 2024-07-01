package org.usf.inspect.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.server.model.InstanceMainSession;
import org.usf.inspect.server.model.InstanceRestSession;

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
