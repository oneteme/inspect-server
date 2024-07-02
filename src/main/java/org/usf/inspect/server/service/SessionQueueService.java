package org.usf.inspect.server.service;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;
import org.usf.inspect.server.model.InstanceSession;

import jakarta.annotation.PreDestroy;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<InstanceSession> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::safeBackup);
    }

    public boolean add(InstanceSession... sessions) {
    	return dispatcher.submit(sessions);
    }

    public List<InstanceSession> waitList(){
    	return dispatcher.peek();
    }

    boolean safeBackup(boolean complete, int attempts, List<InstanceSession> sessions) {
        service.addSessions(sessions);
        return true;
    }
    
    public void enableSave(DispatchState state) {
    	dispatcher.updateState(state);
    }
    
    @PreDestroy
    void destroy() throws InterruptedException {
		dispatcher.complete();
	}
}
