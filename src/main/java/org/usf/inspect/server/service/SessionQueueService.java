package org.usf.inspect.server.service;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchException;
import org.usf.inspect.core.DispatcherAgent;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
//@EnableConfigurationProperties(InspectCollectorConfiguration.class)
@RequiredArgsConstructor
public class SessionQueueService implements DispatcherAgent {
	
	private final RequestService service;
	private final ObjectMapper mapper;

	@Override
	public void dispatch(InstanceEnvironment instance) {
		service.addInstance(null); //cast or change
	}

	@Override
	public void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces) {
		service.addEventTraces(traces);
	}

	@Override
	public void dispatch(File dumpFile) { //TODO dump file dispatch attempts !?
		try {
			var traces = mapper.readValue(dumpFile, EventTrace[].class);
			dispatch(false, 0, 0, asList(traces));
		} catch (IOException e) {
			throw new DispatchException("cannot dispatch dumpFile " + dumpFile.getName(), e);
		}
	}
}
