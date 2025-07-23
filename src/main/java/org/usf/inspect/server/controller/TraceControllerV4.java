package org.usf.inspect.server.controller;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
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

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.EventTraceScheduledDispatcher;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.SessionManager;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.service.RequestService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceControllerV4 {

    private final RequestService requestService;
    private final EventTraceScheduledDispatcher dispatcher;
    private final ExecutorService executor = Executors.newFixedThreadPool(15);

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions( @PathVariable("id") String id,
                                             @RequestBody EventTrace[] sessions,
                                             @RequestParam(required = false)  Integer pending,
                                             @RequestParam(required = false) int attempts,
                                             @RequestParam(required = false) Instant end
    ) {
        try {
            if(end != null){
                requestService.updateInstance(end, id);
            }
            executor.submit(()-> requestService.addInstanceTrace(new InstanceTrace(pending, attempts, sessions.length, now(), id))); //now !!!???
            if(dispatcher.emitAll(sessions)) {
            	return accepted().build();
            }
        }
        catch (Exception e) {
            log.error("trace session", e);
            return internalServerError().build();
        }
        return status(SERVICE_UNAVAILABLE).build();
    }
    
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody InstanceEnvironment instance) {
    	/*if(queueService.getState() == DISABLE) {
            return status(SERVICE_UNAVAILABLE).build();
        }*/
        if(isNull(instance) || isBlank(instance.getName())) { //env !?
    		return status(BAD_REQUEST).build();
    	}
        if(instance.getType() == CLIENT) {
        	update(instance, hsr.getRemoteAddr(), nextId());
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
    
    static InstanceEnvironment update(InstanceEnvironment instance, String addrr, String id) {
    	
    	//TODO complete nextId(), setAddrr 
    	
    	return instance;
    }
}
