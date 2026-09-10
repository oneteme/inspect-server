package org.usf.inspect.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TracePacket;
import org.usf.inspect.core.TraceDispatcherHub;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.LogEntry.Level.REPORT;
import static org.usf.inspect.server.JsonUtils.defaultMapper;
import static org.usf.inspect.server.model.TracePacket.newTracePacket;

@Slf4j
@Service
public class TraceService implements ApplicationListener<UnsavedEventTraceEvent> {

    private final TraceDispatcherHub dispatcher;
    private final JdbcTemplate template;

    TraceService(@Qualifier("inspectServerContext") TraceDispatcherHub dispatcher, JdbcTemplate template) {
        this.dispatcher = dispatcher;
        this.template = template;
    }

    @Deprecated(forRemoval = true, since = "1.2")
    public boolean addInstance(InstanceEnvironment instance) {
        return dispatcher.dispatch(instance);
    }

    public boolean addInstance(InstanceEnvironment instance, String namespace) {
        instance.setNamespace(namespace);
        return dispatcher.dispatch(instance);
    }

    public boolean addTraces(UUID id, int seq, int attempts, Instant end, List<EventTrace> traces) throws DispatchProcessingException {
        var now = now();
        var emitted = false;
        if(isNull(traces)) {
            traces = new ArrayList<>();
        }
        traces.add(newTracePacket(now, seq, attempts, id, traces));
        if(nonNull(end)){
            traces.add(new InstanceEnvironmentUpdate(id, end));
        }
        try {
            for(var e : traces) {
                if(e instanceof AbstractRequestSignal req) {
                    req.setInstanceId(id);
                } else if(e instanceof AbstractSessionSignal ses) {
                    ses.setInstanceId(id);
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

    @Deprecated(forRemoval = true, since = "v1.2")
    public List<EventTrace> peekQueue() {
        return dispatcher.peek();
    }

    public void updateState(DispatchState state) {
        log.info("update dispatcher state to {}", state);
        dispatcher.setState(state);
    }

    public DispatchState getDispatcherState() {
        return dispatcher.getState();
    }
    
    public boolean hasBeenTraced(UUID id, int seq) {
		try {
			return dispatcher.peekAsync(q-> q.stream()
					.anyMatch(t-> t instanceof TracePacket pck 
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
                    var report = new LogEntry(now(), REPORT, defaultMapper.writeValueAsString(trace), null);
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