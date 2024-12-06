package org.usf.inspect.server.service;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;
import org.usf.inspect.server.model.ServerSession;

import jakarta.annotation.PreDestroy;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<ServerSession> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::saveSessions);
    }

    public boolean addSessions(ServerSession[] sessions) {
    	return dispatcher.submit(sessions);
    }

    public List<ServerSession> waitList() throws IllegalAccessException {
    	return dispatcher.peek().toList();
    }
    
    public int waitListSize() throws IllegalAccessException {
    	return (int) dispatcher.peek().count();
    }

    boolean saveSessions(boolean complete, int attempts, List<ServerSession> sessions) {
        service.addSessions(sessions);
        return true;
    }
    
    public void enableSave(DispatchState state) throws InterruptedException {
    	dispatcher.updateState(state);
    }
    
    @PreDestroy
    void destroy() {
		dispatcher.complete();
	}
}
