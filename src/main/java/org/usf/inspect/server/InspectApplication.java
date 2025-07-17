package org.usf.inspect.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.server.model.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
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
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(new NamedType(MainSession.class, "main"), new NamedType(RestSession.class, "rest"));

		mapper.registerSubtypes(new NamedType(LogEntry.class, "Log"),
								new NamedType(MachineResourceUsage.class, "rsrc-usg"),
								new NamedType(MainSession.class, "main-ses"),
								new NamedType(RestSession.class, "rest-ses"),
								new NamedType(LocalRequest.class, "locl-req"),
								new NamedType(DatabaseRequest.class, "jdbc-req"),
								new NamedType(RestRequest.class, "rest-req"),
								new NamedType(MailRequest.class, "mail-req"),
								new NamedType(NamingRequest.class, "ldap-req"),
								new NamedType(FtpRequest.class, "ftp-req"),
								new NamedType(DatabaseRequestStage.class, "jdbc-stg"),
								new NamedType(HttpRequestStage.class, "rest-stg"),
								new NamedType(MailRequestStage.class, "mail-stg"),
								new NamedType(NamingRequestStage.class, "ldap-stg"),
								new NamedType(FtpRequestStage.class, "ftp-stg")
				);
		return mapper;
	}
}
