package org.usf.inspect.server;

import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.usf.inspect.core.Dispatcher;
import org.usf.inspect.core.DispatcherAgent;
import org.usf.inspect.core.EventTraceScheduledDispatcher;
import org.usf.inspect.core.SchedulingProperties;
import org.usf.inspect.core.TracingProperties;
import org.usf.inspect.server.model.DatabaseRequest;
import org.usf.inspect.server.model.DatabaseRequestStage;
import org.usf.inspect.server.model.FtpRequest;
import org.usf.inspect.server.model.FtpRequestStage;
import org.usf.inspect.server.model.HttpRequestStage;
import org.usf.inspect.server.model.LocalRequest;
import org.usf.inspect.server.model.LogEntry;
import org.usf.inspect.server.model.MachineResourceUsage;
import org.usf.inspect.server.model.MailRequest;
import org.usf.inspect.server.model.MailRequestStage;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.NamingRequest;
import org.usf.inspect.server.model.NamingRequestStage;
import org.usf.inspect.server.model.RestRequest;
import org.usf.inspect.server.model.RestSession;

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
	ObjectMapper mapper(){
		var mapper = json()
				.modules(new JavaTimeModule(), new ParameterNamesModule())
				.build()
			    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // !null & !empty
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(new NamedType(MainSession.class, "main"), new NamedType(RestSession.class, "rest"));

		mapper.registerSubtypes(
				new NamedType(LogEntry.class, 				"log"),
				new NamedType(MachineResourceUsage.class, 	"rsrc-usg"),
				new NamedType(MainSession.class,  			"main-ses"),
				new NamedType(RestSession.class,  			"rest-ses"),
				new NamedType(LocalRequest.class, 			"locl-req"),
				new NamedType(DatabaseRequest.class,		"jdbc-req"),
				new NamedType(RestRequest.class,  			"http-req"),
				new NamedType(MailRequest.class,  			"mail-req"),
				new NamedType(NamingRequest.class,			"ldap-req"),
				new NamedType(FtpRequest.class,  			"ftp-req"),
				new NamedType(DatabaseRequestStage.class,	"jdbc-stg"),
				new NamedType(HttpRequestStage.class,  		"http-stg"),
				new NamedType(MailRequestStage.class,  		"mail-stg"),
				new NamedType(NamingRequestStage.class,		"ldap-stg"),
				new NamedType(FtpRequestStage.class,  		"ftp-stg"));
		return mapper;
	}
	
	@Bean
	Dispatcher dispatcher(DispatcherAgent agent) {
		var trc = new TracingProperties();
		trc.setDelayIfPending(0); //save immediately
		trc.setQueueCapacity(1_000_000);
		var scd = new SchedulingProperties();
		scd.setDelay(30); //30s
		return new EventTraceScheduledDispatcher(trc, scd, agent);
	}
}
