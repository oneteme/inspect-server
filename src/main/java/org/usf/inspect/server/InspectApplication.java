package org.usf.inspect.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.wrapper.*;

import java.util.List;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

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
				.modules(new JavaTimeModule(), new ParameterNamesModule())
				.build()
			    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // !null & !empty
		mapper.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true);
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(new NamedType(MainSessionWrapper.class, "main"), new NamedType(RestSessionWrapper.class, "rest"));

		mapper.registerSubtypes(
				new NamedType(LogEntry.class, 					"log"),
				new NamedType(MachineResourceUsage.class,		"rsrc-usg"),
				new NamedType(MainSession.class,  				"main-ses"),
				new NamedType(RestSession.class,  				"rest-ses"),
				new NamedType(LocalRequest.class, 				"locl-req"),
				new NamedType(DatabaseRequest.class,			"jdbc-req"),
				new NamedType(RestRequest.class,  				"http-req"),
				new NamedType(MailRequest.class,  				"mail-req"),
				new NamedType(DirectoryRequest.class,			"ldap-req"),
				new NamedType(FtpRequest.class,  				"ftp-req"),
				new NamedType(DatabaseRequestStage.class,		"jdbc-stg"),
				new NamedType(HttpRequestStage.class,  			"http-stg"),
				new NamedType(HttpSessionStage.class,  			"sess-stg"),
				new NamedType(MailRequestStage.class,  			"mail-stg"),
				new NamedType(DirectoryRequestStage.class,		"ldap-stg"),
				new NamedType(FtpRequestStage.class,  			"ftp-stg"),
				new NamedType(RestRemoteServerProperties.class,	"rest-rmt"));
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
		var dump = new EventTraceDumper(conf.getTracing().getDump().getLocation(), mapper);
		return new EventTraceScheduledDispatcher(conf.getTracing(), conf.getScheduling(), agent, List.of(dump));
	}
}
