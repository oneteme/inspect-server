package org.usf.trace.api.server.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.traceapi.core.ScheduledSessionDispatcher;
import org.usf.traceapi.core.Session;
import org.usf.traceapi.core.SessionDispatcherProperties;
import org.usf.traceapi.core.State;

import jakarta.annotation.PreDestroy;

@Service
@EnableConfigurationProperties(SessionDispatcherProperties.class)
public class SessionQueueService {

    private final JqueryRequestService service;
    private final ScheduledSessionDispatcher dispatcher;
    
    public SessionQueueService(JqueryRequestService service, SessionDispatcherProperties prop) {
    	this.service = service;
		this.dispatcher = new ScheduledSessionDispatcher(prop, this::safeBackup);
    }
    
    public boolean add(Session... sessions) {
    	return dispatcher.add(sessions);
    }
    
    public List<Session> waitList(){
    	return dispatcher.peekSessions(); // send copy
    }

    boolean safeBackup(int attempts, List<Session> sessions) {
        service.addSessions(sessions);
        return true;
    }
    
    public void enableSave(State state) {
    	dispatcher.updateState(state);
    }
    
    @PreDestroy
    void destroy() throws InterruptedException {
		dispatcher.shutdown();
	}
}
