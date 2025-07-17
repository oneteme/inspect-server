package org.usf.inspect.server.service;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;

import jakarta.annotation.PreDestroy;
import org.usf.inspect.server.model.Traceable;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<Traceable> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::saveTraceables);
    }

    public boolean addTraceables(Traceable... traceables) {
    	return dispatcher.submit(traceables);
    }

    public List<Traceable> waitList() {
    	return dispatcher.peek().toList();
    }

    public int waitListSize() {
    	return (int) dispatcher.peek().count();
    }

    boolean saveTraceables(boolean complete, int attempts, List<Traceable> traceables, int pending) {
        service.addTraceables(traceables);
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
