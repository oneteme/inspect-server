package org.usf.inspect.server.service;

import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.server.model.TraceBatch.newTraceBatch;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.serverHub;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.usf.inspect.AbstractRequestSignal;
import org.usf.inspect.AbstractSessionSignal;
import org.usf.inspect.EventTrace;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.ReportEvent;
import org.usf.inspect.SessionEvent;
import org.usf.inspect.SystemMetrics;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.TraceDispatchException;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.exception.TraceDispatchConflictException;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TraceService {

    private final JdbcTemplate template;

    public void addInstance(InstanceEnvironment instance, int attempts) throws TraceDispatchException {
		if(getDispatcherState() == DISABLE) { //avoid checking the database if the dispatcher is disabled
			throw new TraceDispatchException("trace dispatcher is disabled");
		}
    	if(attempts > 1 && hasInstance(instance.getId())) {
			throw new TraceDispatchConflictException("instance with id=%s has already been registered".formatted(instance.getId()));
		}
    	serverHub().dispatch(instance); //dispatch immediately 
        serverHub().emitTrace(newTraceBatch(now(), 0, attempts, instance.getId(), emptyList())); //ensure that the trace batch is emitted after the instance has been dispatched
    }

    @TraceableStage
    public void addTraces(UUID id, int seq, int attempts, Instant end, List<EventTrace> traces) throws TraceDispatchException {
        if(getDispatcherState() == DISABLE) { //avoid checking the database if the dispatcher is disabled
			throw new TraceDispatchException("trace dispatcher is disabled");
		}
        var now = now();
		if(attempts > 1 && hasBeenTraced(id, seq)) {
			throw new TraceDispatchConflictException("trace batch with sequence=%s for instance.id=%s has already been processed".formatted(seq, id));
		}
    	if(nonNull(end)){
        	serverHub().emitTrace(new InstanceEnvironmentUpdate(id, end));
        }
        if(nonNull(traces) && !traces.isEmpty()) {
            for(var e : traces) {
                if(e instanceof AbstractRequestSignal sgn) {
                    sgn.setInstanceId(id);
                } 
                else if(e instanceof AbstractSessionSignal sgn) {
                    sgn.setInstanceId(id);
                } 
                else if(e instanceof SystemMetrics sys) {
                    sys.setInstanceId(id);
                } 
                else if(e instanceof ReportEvent evn) {
                    evn.setInstanceId(id);
                }
                else if(e instanceof SessionEvent se) {
                	se.setInstanceId(id);
                }
            }
            serverHub().emitTraces(traces);
        }
        serverHub().emitTrace(newTraceBatch(now, seq, attempts, id, traces)); //ensure that the trace batch is emitted after all traces have been dispatched
	}

    public Collection<EventTrace> peekQueue() {
        try {
			return serverHub().peekAsync(identity()).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while peeking trace queue", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Failed to peek trace queue", e.getCause());
		}
    }

    public DispatchState getDispatcherState() {
        return serverHub().getState();
    }
    
    public void updateState(DispatchState state) {
        serverHub().setState(state);
        log.info("Trace dispatcher state updated to {}", state);
    }

    public boolean hasInstance(UUID id) {
        return template.queryForObject(
                "SELECT COUNT(id_ins) FROM e_env_ins WHERE id_ins=?",
                (rs, idx) -> rs.getInt(1), id) > 0;
    }
    
    public boolean hasBeenTraced(UUID id, int seq) {
		try {
			//make sure that the trace has been processed by the dispatcher before checking the database
			return serverHub().peekAsync(q-> q.stream()
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
}