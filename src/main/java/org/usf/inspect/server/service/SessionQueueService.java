package org.usf.inspect.server.service;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.InspectConfigurationProperties;
import org.usf.inspect.core.ScheduledDispatchHandler;
import org.usf.inspect.server.model.Metric;

import jakarta.annotation.PreDestroy;

@Service
@EnableConfigurationProperties(InspectConfigurationProperties.class)
public class SessionQueueService {

    private final RequestService service;
    private final ScheduledDispatchHandler<Metric> dispatcher;

    public SessionQueueService(RequestService service, InspectConfigurationProperties prop) {
        this.service = service;
		this.dispatcher = new ScheduledDispatchHandler<>(prop.getDispatch(), this::saveMetrics);
    }

    public boolean addMetrics(Metric... metrics) {
    	return dispatcher.submit(metrics);
    }

    public List<Metric> waitList() {
    	return dispatcher.peek().toList();
    }
    
    public int waitListSize() {
    	return (int) dispatcher.peek().count();
    }

    boolean saveMetrics(boolean complete, int attempts, List<Metric> metrics, int pending) {
        service.addMetrics(metrics);
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
