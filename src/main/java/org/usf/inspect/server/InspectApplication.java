package org.usf.inspect.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.wrapper.*;

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
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(new NamedType(MainSessionWrapper.class, "main"), new NamedType(RestSessionWrapper.class, "rest"));

		mapper.registerSubtypes(
				new NamedType(LogEntryWrapper.class, 			"log"),
				new NamedType(MachineResourceUsageWrapper.class,"rsrc-usg"),
				new NamedType(MachineResourceUsage.class,		"rsrc-usg"),
				new NamedType(MainSessionWrapper.class,  		"main-ses"),
				new NamedType(RestSessionWrapper.class,  		"rest-ses"),
				new NamedType(LocalRequestWrapper.class, 		"locl-req"),
				new NamedType(DatabaseRequestWrapper.class,		"jdbc-req"),
				new NamedType(RestRequestWrapper.class,  		"http-req"),
				new NamedType(MailRequestWrapper.class,  		"mail-req"),
				new NamedType(NamingRequestWrapper.class,		"ldap-req"),
				new NamedType(FtpRequestWrapper.class,  		"ftp-req"),
				new NamedType(DatabaseRequestStageWrapper.class,		"jdbc-stg"),
				new NamedType(HttpRequestStageWrapper.class,  			"http-stg"),
				new NamedType(HttpSessionStageWrapper.class,  			"sess-stg"),
				new NamedType(MailRequestStageWrapper.class,  			"mail-stg"),
				new NamedType(NamingRequestStageWrapper.class,			"ldap-stg"),
				new NamedType(FtpRequestStageWrapper.class,  			"ftp-stg"),
				new NamedType(RestRemoteServerProperties.class, "rest-rmt"));
		return mapper;
	}
	
	@Bean
	EventTraceScheduledDispatcher dispatcher(DispatcherAgent agent, ObjectMapper mapper) {
		var trc = new TracingProperties();
		trc.setDelayIfPending(0); //save immediately
		trc.setQueueCapacity(1_000_000);
		var scd = new SchedulingProperties();
		scd.setInterval(ofSeconds(30)); //30s
		var dump = new EventTraceDumper(trc.getDump().getLocation(), mapper);
		return new EventTraceScheduledDispatcher(trc, scd, agent, asList(dump));
	}
}
