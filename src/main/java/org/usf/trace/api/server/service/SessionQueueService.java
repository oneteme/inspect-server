package org.usf.trace.api.server.service;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.trace.api.server.service.v3.JqueryV3RequestService;
import org.usf.traceapi.core.ScheduledDispatcher;
import org.usf.traceapi.core.SessionDispatcherProperties;
import org.usf.traceapi.core.State;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.synchronizedList;

@Service
@EnableConfigurationProperties(SessionDispatcherProperties.class)
public class SessionQueueService {

    private final JqueryV3RequestService service;
    private final ScheduledDispatcher<InstanceSession> dispatcher;
    private final List<InstanceEnvironmentWrapper> instances = synchronizedList(new ArrayList<>(10));
    
    public SessionQueueService(JqueryV3RequestService service, SessionDispatcherProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatcher<>(prop, this::safeBackup);
    }

    public boolean add(InstanceSession... sessions) {
    	return dispatcher.add(sessions);
    }

    public boolean add(InstanceEnvironmentWrapper instance) {
        return instances.add(instance);
    }

    public List<InstanceSession> waitList(){
    	return dispatcher.peek(); // send copy
    }

    boolean safeBackup(int attempts, List<InstanceSession> sessions) {
        if(!instances.isEmpty()) {
            var copy = new ArrayList<>(instances);
            service.addInstanceEnvironment(copy);
            instances.subList(0, copy.size()).clear();
        }
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
