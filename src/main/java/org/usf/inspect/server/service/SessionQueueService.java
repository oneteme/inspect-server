package org.usf.inspect.server.service;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatcherAgent;
import org.usf.inspect.core.EventTrace;
//import org.usf.inspect.core.ScheduledDispatchHandler;
import org.usf.inspect.core.InstanceEnvironment;

import lombok.RequiredArgsConstructor;

@Service
//@EnableConfigurationProperties(InspectCollectorConfiguration.class)
@RequiredArgsConstructor
public class SessionQueueService implements DispatcherAgent {
	
	private final RequestService service;
	

	@Override
	public void register(InstanceEnvironment instance) {
		
	}

	@Override
	public void dispatch(boolean complete, int attemps, int pending, List<EventTrace> traces) {
		service.addEventTraces(traces);
	}

	@Override
	public void dispatch(File dumpFile) {
		
	}
}
