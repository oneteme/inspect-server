package org.usf.inspect.server.controller;

import static java.time.Instant.now;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.jquery.core.Utils.isBlank;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.core.AbstractRequest;
import org.usf.inspect.core.AbstractSession;
import org.usf.inspect.core.BasicDispatchState;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.EventTraceScheduledDispatcher;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.LogEntry;
import org.usf.inspect.core.MachineResourceUsage;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.service.TraceService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

	private static final ExecutorService executor = newFixedThreadPool(15);

	private final TraceService traceService;
    private final EventTraceScheduledDispatcher dispatcher;
    
    private static final EventTrace[] EMTY_TRACE = new EventTrace[0]; 
    
    @PostMapping("instance")
    public ResponseEntity<String> addInstanceEnvironment(
    		HttpServletRequest hsr,
            @RequestBody InstanceEnvironment instance) {
        if(isBlank(instance.getName())) { //env !?
    		return status(BAD_REQUEST).build();
    	}
        if(instance.getType() == CLIENT) {
        	instance = update(instance, hsr.getRemoteAddr(), nextId());
        }
        try {
    		return dispatcher.dispatch(instance) 
    		? ok(instance.getId())
        	: status(SERVICE_UNAVAILABLE).build();
        } catch(Exception e) {
        	log.error("post instance", e);
        	return internalServerError().build();
        }
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(
            @PathVariable String id,
            @RequestParam(required = false) Integer pending, //TODO Integer -> int
            @RequestParam(required = false) Integer attempts, //TODO Integer -> int
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) String filename,
            @RequestBody EventTrace[] body) { 
    	var now = now();
        try {
            if(end != null){
                executor.submit(()-> traceService.updateInstance(end, id)); //dispatch.state = PROPAGE|DISABLE
            }
            var traces = body == null ? EMTY_TRACE : body; //avoid NullPointerException   
            executor.submit(()-> traceService.addInstanceTrace(new InstanceTrace(pending, attempts, traces.length, filename, now, id))); //dispatch.state = PROPAGE|DISABLE
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
            return dispatcher.emitAll(traces) 
                		? accepted().build()
        				: status(SERVICE_UNAVAILABLE).build();
        }
        catch (Exception e) {
            log.error("put sessions", e);
            return internalServerError().build();
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
