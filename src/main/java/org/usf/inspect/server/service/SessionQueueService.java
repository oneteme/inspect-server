package org.usf.inspect.server.service;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;
import org.usf.inspect.server.model.Session;

import java.util.List;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<Session> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::saveSessions);
    }

    public boolean addSessions(Session[] sessions) {
    	return dispatcher.submit(sessions);
    }

    public List<Session> waitList() {
    	return dispatcher.peek().toList();
    }
    
    public int waitListSize() {
    	return (int) dispatcher.peek().count();
    }

    boolean saveSessions(boolean complete, int attempts, List<Session> sessions) {
        service.addSessions(sessions);
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
