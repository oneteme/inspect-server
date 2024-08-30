package org.usf.inspect.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.server.model.ServerMainSession;
import org.usf.inspect.server.model.ServerRestSession;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

@SpringBootApplication
@EnableTransactionManagement
public class InspectApplication {

	public static void main(String[] args) {
		SpringApplication.run(InspectApplication.class, args);
	}

	@Bean
	@Primary
	public ObjectMapper mapper(){
		var mapper = json()
				.modules(new JavaTimeModule(), new ParameterNamesModule())
				.build()
			    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // !null & !empty
		mapper.registerSubtypes(ServerRestSession.class, ServerMainSession.class);
		return mapper;
	}
}
