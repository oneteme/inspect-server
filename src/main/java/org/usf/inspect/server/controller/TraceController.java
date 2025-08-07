package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdated;
import org.usf.inspect.server.model.InstanceTrace;

import java.time.Instant;
import java.util.List;

import static java.lang.System.arraycopy;
import static java.time.Instant.now;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.jquery.core.Utils.isBlank;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private static final EventTrace[] EMPTY_TRACE = new EventTrace[0];

    private final EventTraceScheduledDispatcher dispatcher;
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
		HttpServletRequest hsr,
		@RequestBody InstanceEnvironment instance) {
    	if(isBlank(instance.getName())) { //env !?
    		return status(BAD_REQUEST).body("invalid instance name");
    	}
    	if(instance.getType() == CLIENT) {
    		instance = update(instance, hsr.getRemoteAddr(), nextId());
    	}
    	try {
    		return dispatcher.dispatch(instance) 
    				? ok(instance.getId())
    				: status(SERVICE_UNAVAILABLE).body("dispatcher.state=" + dispatcher.getState());
    	} catch(Exception e) {
    		log.error("post instance", e);
    		return internalServerError().body("unexpected exception " + e.getClass().getSimpleName());
    	}
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Object> addSessions(
    		@PathVariable String id,
            @RequestParam(required = false) Integer pending,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody EventTrace[] body) {
    	var now = now();
    	try {
    		var traces = nonNull(body) ? body : EMPTY_TRACE ; //avoid NullPointerException
    		var copy = new EventTrace[traces.length + (nonNull(end) ? 2 : 1)];
    		arraycopy(traces, 0, copy, 0, traces.length);
    		if(nonNull(end)){
    			copy[copy.length - 2] = new InstanceEnvironmentUpdated(id, end);
    		}
    		copy[copy.length - 1] = new InstanceTrace(pending, attempts, traces.length, filename, now, id);
    		for(var e : traces) {
    			if(e instanceof AbstractRequest req) {
    				req.setInstanceId(id);
    			} else if(e instanceof AbstractSession ses) {
    				ses.setInstanceId(id);
    			} else if(e instanceof MachineResourceUsage usg) {
    				usg.setInstanceId(id);
    			} else if(e instanceof LogEntry ent) {
    				ent.setInstanceId(id);
    			} //stages dosn't need instance id
    		}
    		return dispatcher.emitAll(copy)
    				? accepted().build()
    				: status(SERVICE_UNAVAILABLE).body("dispatcher.state=" + dispatcher.getState());
    	}
    	catch (Exception e) {
    		log.error("put sessions", e);
    		return internalServerError().body("unexpected exception " + e.getClass().getSimpleName());
    	}
    }
    
    @GetMapping("queue")
    public List<EventTrace> peekQueue(){
		return dispatcher.peek().toList();
    }

    @PostMapping("state/{state}")
    public void updateState(@PathVariable BasicDispatchState state){
		dispatcher.setState(state);
    }

    static InstanceEnvironment update(InstanceEnvironment instance, String addr, String id) {
    	var ins = new InstanceEnvironment(id, instance.getInstant(), instance.getType(),
                instance.getName(), instance.getVersion(), instance.getEnv(), addr,
                instance.getOs(), instance.getRe(), instance.getUser(), instance.getBranch(),
                instance.getHash(), instance.getCollector(),
                instance.getAdditionalProperties(), instance.getConfiguration());
        ins.setResource(instance.getResource());
        ins.setEnd(instance.getEnd());
    	return ins;
    }
}
