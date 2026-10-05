package org.usf.inspect.server.service;

import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static org.usf.inspect.server.JsonUtils.defaultMapper;
import static org.usf.inspect.server.model.TraceBatch.newTraceBatch;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.AbstractRequestSignal;
import org.usf.inspect.core.AbstractSessionSignal;
import org.usf.inspect.core.DispatchException;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.MachineResourceUsage;
import org.usf.inspect.core.ReportEvent;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TraceService implements ApplicationListener<UnsavedEventTraceEvent> {

    private final TraceBatchDispatcherHub dispatcher;
    private final JdbcTemplate template;

    TraceService(TraceBatchDispatcherHub dispatcher, JdbcTemplate template) {
        this.dispatcher = dispatcher;
        this.template = template;
    }

    public void addInstance(InstanceEnvironment instance, String namespace, int attempts) throws DispatchException {
        instance.setNamespace(namespace);
        dispatcher.dispatch(instance);
        dispatcher.emitTrace(newTraceBatch(now(), 0, attempts, instance.getId(), emptyList())); //ensure that the trace batch is emitted after the instance has been dispatched
    }

    public void addTraces(UUID id, int seq, int attempts, Instant end, List<EventTrace> traces) throws DispatchProcessingException {
        var now = now();
    	if(nonNull(end)){
        	dispatcher.emitTrace(new InstanceEnvironmentUpdate(id, end));
        }
        if(nonNull(traces) && !traces.isEmpty()) {
            for(var e : traces) {
                if(e instanceof AbstractRequestSignal sgn) {
                    sgn.setInstanceId(id);
                } else if(e instanceof AbstractSessionSignal sgn) {
                    sgn.setInstanceId(id);
                } else if(e instanceof MachineResourceUsage mru) {
                    mru.setInstanceId(id);
                } else if(e instanceof ReportEvent evn) {
                    evn.setInstanceId(id);
                }
            }
            dispatcher.emitTraces(traces);
        }
        dispatcher.emitTrace(newTraceBatch(now, seq, attempts, id, traces)); //ensure that the trace batch is emitted after all traces have been dispatched
	}

    public Collection<EventTrace> peekQueue() {
        try {
			return dispatcher.peekAsync(identity()).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while peeking trace queue", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to peek trace queue", e.getCause());
		}
    }

    public DispatchState getDispatcherState() {
        return dispatcher.getState();
    }
    
    public void updateState(DispatchState state) {
        log.info("update dispatcher state to {}", state);
        dispatcher.setState(state);
    }
    
    public boolean hasBeenTraced(UUID id, int seq) {
		try {
			//make sure that the trace has been processed by the dispatcher before checking the database
			return dispatcher.peekAsync(q-> q.stream()
					.anyMatch(t-> t instanceof TraceBatch pck 
						&& pck.getInstanceId().equals(id) 
						&& pck.getSequence() == seq)).get() || 
					template.queryForObject("SELECT COUNT(*) FROM e_ins_trc WHERE cd_ins=? AND va_seq=?", 
							(rs,idx)-> rs.getInt(1), id, seq) > 0;
		} catch (InterruptedException e) {
	        currentThread().interrupt();
	        throw new IllegalStateException("Interrupted while checking trace sequence " + seq + " for instance " + id, e);
	    } catch (ExecutionException e) {
	        throw new IllegalStateException("Failed to inspect trace queue for instance " + id, e.getCause());
	    }
    }

    @Override
    public void onApplicationEvent(UnsavedEventTraceEvent event) {
        var trace = event.getTrace();
        if(event.isRetry()) {
            dispatcher.emitTrace(trace);
        }
        else {
            UUID id = null;
            if(trace instanceof AbstractSessionSignal s) {
                id = s.getInstanceId();
            }
            else if(trace instanceof AbstractRequestSignal r) {
                id = r.getInstanceId();
            }
            if(nonNull(id)) {
                try {
                    var report = new ReportEvent(now(), null, "saveTrace", defaultMapper.writeValueAsString(trace), null);
                    report.setInstanceId(id);
                    dispatcher.emitTrace(report);
                }
                catch(Exception e) {
                    log.warn("cannot report unsaved trace of type {} because of serialization error: {}", trace.getClass().getSimpleName(), e.getMessage());
                }
            }
            else {
                log.warn("cannot report unsaved trace of type {} because instanceId is missing", trace.getClass().getSimpleName());
            }
        }
    }
}