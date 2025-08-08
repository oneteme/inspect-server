package org.usf.inspect.server.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchException;
import org.usf.inspect.core.DispatcherAgent;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DatabaseDispatcherAgent implements DispatcherAgent {

	private final TraceService service;
	private final ObjectMapper mapper;

	@Override
	public void dispatch(InstanceEnvironment instance) {
		service.addInstance(instance);
	}

	@Override
	public List<EventTrace> dispatch(boolean complete, int attempts, int pending, List<EventTrace> traces) {
		return service.addTraces(traces);
	}

	@Override
	public void dispatch(int attempts, File dumpFile) {
		try {
			var traces = mapper.readValue(dumpFile, new TypeReference<List<EventTrace>>() {});
			dispatch(false, attempts, 0, traces);
		} catch (IOException e) {
			throw new DispatchException("cannot dispatch dumpFile " + dumpFile.getName(), e);
		}
	}
}
