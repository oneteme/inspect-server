package org.usf.inspect.server.service;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;

import jakarta.annotation.PreDestroy;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<EventTrace> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::saveEventTraces);
    }

    public boolean addEventTraces(EventTrace... eventTraces) {
    	return dispatcher.submit(eventTraces);
    }

    public List<EventTrace> waitList() {
    	return dispatcher.peek().toList();
    }

    public int waitListSize() {
    	return (int) dispatcher.peek().count();
    }

    boolean saveEventTraces(boolean complete, int attempts, List<EventTrace> eventTraces, int pending) {
        service.addEventTraces(traceables);
        return true;
    }
    
    public void enableSave(DispatchState state) {
    	dispatcher.updateState(state);
    }

    public DispatchState getState() {
        return dispatcher.getState();
    }

    @PreDestroy
    void destroy() {
		dispatcher.complete();
	}
}
