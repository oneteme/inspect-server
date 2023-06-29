package org.usf.trace.api.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
		return json()
				.build()
				.registerModules(new JavaTimeModule(), new ParameterNamesModule());
	}
}
