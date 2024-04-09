package org.usf.trace.api.server;

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

    private final RequestDao dao;
    private final ScheduledSessionDispatcher dispatcher;
    
    public SessionQueueService(RequestDao dao, SessionDispatcherProperties prop) {
    	this.dao = dao;
		this.dispatcher = new ScheduledSessionDispatcher(prop, this::safeBackup);
    }
    
    public boolean add(Session... sessions) {
    	return dispatcher.add(sessions);
    }
    
    public List<Session> waitList(){
    	return dispatcher.peekSessions(); // send copy
    }

    @Deprecated(forRemoval = true)
    public Collection<Session> deleteSessions(Set<String> ids){
    	throw new UnsupportedOperationException();
    }

    boolean safeBackup(int attempts, List<Session> sessions) {
        dao.saveSessions(sessions);
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
