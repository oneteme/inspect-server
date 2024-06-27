package org.usf.trace.api.server.service;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.traceapi.core.DispatchState;
import org.usf.traceapi.core.InspectConfigurationProperties;
import org.usf.traceapi.core.ScheduledDispatchHandler;

import java.util.List;

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
