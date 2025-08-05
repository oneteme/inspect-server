package org.usf.inspect.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchException;
import org.usf.inspect.core.DispatcherAgent;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DatabaseDispatcherAgent implements DispatcherAgent {

	private final TraceService traceService;
	private final ObjectMapper mapper;

	@Override
	public void dispatch(InstanceEnvironment instance) {
		traceService.addInstance(instance);
	}

	@Override
	public Collection<EventTrace> dispatch(boolean complete, int attempts, int pending, EventTrace[] traces) {
		return traceService.addTraces(Arrays.asList(traces));
	}

	@Override
	public void dispatch(int attempts, File dumpFile) {
		try {
			var traces = mapper.readValue(dumpFile, new TypeReference<EventTrace[]>() {});
			dispatch(false, attempts, 0, traces);
		} catch (IOException e) {
			throw new DispatchException("cannot dispatch dumpFile " + dumpFile.getName(), e);
		}
	}
}
