package org.usf.inspect.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.LogEntry.Level.REPORT;
import static org.usf.inspect.server.JsonUtils.safeWriteValue;
import static org.usf.inspect.server.Utils.assertUUID;
import static org.usf.inspect.server.model.TraceBatchResolver.resolve;
import static org.usf.inspect.server.service.TracePersistenceService.filterAndApply;

@Slf4j
@Service
public class TraceService implements ApplicationListener<UnsavedEventTraceEvent> {
	
    private final TraceDispatcherHub dispatcher;
    private final ObjectMapper mapper;

    public TraceService(@Qualifier("inspectServerContext") TraceDispatcherHub dispatcher, ObjectMapper mapper) {
        this.dispatcher = dispatcher;
        this.mapper = mapper;
    }

    public boolean addInstance(InstanceEnvironment instance) {
        return dispatcher.dispatch(instance);
    }

    public boolean addTraces(List<EventTrace> traces, String id, Integer attempts, String filename, Instant end) throws DispatchProcessingException {
        var now = now();
        var emitted = false;
        try {
            if(isNull(traces)) {
                traces = new ArrayList<>();
            }
            InstanceTrace instanceTrace = new InstanceTrace(attempts, filename, now, id);
            resolve(traces, instanceTrace);
            filterAndApply(traces, AbstractStage.class, t -> instanceTrace.addTraceCount(t.size()));
            traces.add(instanceTrace);
            if(nonNull(end)){
                traces.add(new InstanceEnvironmentUpdate(id, end));
            }
            for(var e : traces) {
                if(e instanceof AbstractRequestSignal req) {
                    req.setInstanceId(id);
                    assertUUID(req.getId(), "req.id");
                } else if(e instanceof AbstractSessionSignal ses) {
                    ses.setInstanceId(id);
                    assertUUID(ses.getId(), "ses.id");
                } else if(e instanceof MachineResourceUsage usg) {
                    usg.setInstanceId(id);
                } else if(e instanceof LogEntry ent) {
                    ent.setInstanceId(id);
                }
            }
            emitted = true;
            return dispatcher.emitTraces(traces);
        } catch(Throwable e) { //OutOfMem
            throw new DispatchProcessingException(!emitted, e);
        }
    }

    public List<EventTrace> peekQueue() {
        return dispatcher.peek();
    }

    public void updateState(DispatchState state) {
        log.info("update dispatcher state to {}", state);
        dispatcher.setState(state);
    }

    public DispatchState getState() {
        return dispatcher.getState();
    }

    @Override
    public void onApplicationEvent(UnsavedEventTraceEvent event) {
    	var trace = event.getTrace();
    	if(event.isRetry()) {
        	dispatcher.emitTrace(trace);
    	}
	 	else {
            String id = null;
            if(trace instanceof AbstractSessionSignal o) {
            	id = o.getInstanceId();
            }
            else if(trace instanceof AbstractRequestSignal o) {
            	id = o.getInstanceId();
            }
            if(nonNull(id)) {
                var report = new LogEntry(now(), REPORT, safeWriteValue(event.getTrace(), mapper), null);
                report.setInstanceId(id);
			}
            else {
            	log.warn("cannot report unsaved trace of type {} because instanceId is missing", trace.getClass().getSimpleName());
            }
    	}
    }
}
