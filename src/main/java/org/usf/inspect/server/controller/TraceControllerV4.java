package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.TraceService;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.jquery.core.Utils.isBlank;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceControllerV4 {

    private final TraceService traceService;
    private final EventTraceScheduledDispatcher dispatcher;
    private final ExecutorService executor = Executors.newFixedThreadPool(15);
    
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
            HttpServletRequest hsr,
            @RequestBody InstanceEnvironment instance) {
        if(isNull(instance) || isBlank(instance.getName())) { //env !?
    		return status(BAD_REQUEST).build();
    	}
        if(instance.getType() == CLIENT) {
        	instance = update(instance, hsr.getRemoteAddr(), nextId());
        }
        try {
        	if(dispatcher.dispatch(instance)) {
        		return ok(instance.getId()); //optional 
        	}
        } catch(Exception e) {
        	log.error("trace instance", e);
        	return internalServerError().build();
        }
        return status(SERVICE_UNAVAILABLE).build();
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(
            @PathVariable("id") String id,
            @RequestParam(required = false) Integer pending,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) Instant end,
            @RequestParam(required = false) String fileName,
            @RequestBody EventTrace[] eventTraces) {
        try {
            if(end != null){
                executor.submit(()-> traceService.updateInstance(end, id));
            }
            executor.submit(()-> traceService.addInstanceTrace(new InstanceTrace(pending, attempts, eventTraces.length, fileName, now(), id))); //now !!!???

            Arrays.stream(eventTraces).forEach(e -> {
                if(e instanceof AbstractRequest ie) {
                    ie.setInstanceId(id);
                } else if(e instanceof AbstractSession ie) {
                    ie.setInstanceId(id);
                } else if(e instanceof LogEntry ie) {
                    ie.setInstanceId(id);
                } else if(e instanceof MachineResourceUsage ie) {
                    ie.setInstanceId(id);
                }
            });

            if(dispatcher.emitAll(eventTraces)) {
                return accepted().build();
            }
        }
        catch (Exception e) {
            log.error("trace session", e);
            return internalServerError().build();
        }
        return status(SERVICE_UNAVAILABLE).build();
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
