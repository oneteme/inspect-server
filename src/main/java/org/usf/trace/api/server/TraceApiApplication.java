package org.usf.trace.api.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@SpringBootApplication
public class TraceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceApiApplication.class, args);
	}

	@Primary
	@Bean
	public ObjectMapper mapper(){
		return Jackson2ObjectMapperBuilder
				.json()
				.build()
				.registerModules(new JavaTimeModule(), new ParameterNamesModule());
	}
}
