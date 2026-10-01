package org.usf.inspect.server.controller;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.http.WebUtils.TRACE_RETRY_HEADER;
import static org.usf.jquery.core.Utils.isEmpty;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.service.TraceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(exposedHeaders = TRACE_RETRY_HEADER)
@RequestMapping(value = "/v5/trace", produces = APPLICATION_JSON_VALUE)
public class TraceController {
    
    private static final String DO_RETRY = "1";
    private static final String DO_NOT_RETRY = "0";

    private final TraceService service;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> addInstanceEnvironment(
    		@RequestBody InstanceEnvironment instance, 
    		Principal principal){ //check
    	
    	if(service.getDispatcherState() == DISABLE) {
        	return status(SERVICE_UNAVAILABLE)
        			.body("dispatch.state=DISABLE");
    	}
        if(isEmpty(instance.getName())) {
            return status(BAD_REQUEST).body("invalid name="+instance.getName());
        }
        if (instance.getId() == null){
            return status(BAD_REQUEST).body("invalid id="+instance.getId());
        }
        try {
           var nsp = nonNull(principal) 
        		   ? principal.getName()
        		   : instance.getNamespace(); //disabled spring security
            return service.addInstance(instance, nsp)
                    ? ok(instance.getId().toString())
                    : status(SERVICE_UNAVAILABLE).body("dispatcher.state=" + service.getDispatcherState());
        } catch(Exception e) {
            log.error("failed to add instance environment", e);
            return internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Object> addTraces(
            @PathVariable UUID id,
            @RequestParam int seq, //24*60*4 * 365*10 < Integer.MAX_VALUE
            @RequestParam int atm, //TODO check non null !?
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces){
    	
    	if(service.getDispatcherState() == DISABLE) {
        	return status(SERVICE_UNAVAILABLE)
        			.header(TRACE_RETRY_HEADER, DO_RETRY)
        			.body("dispatch.state=DISABLE");
    	}
        try {
        	if(atm > 1 && service.hasBeenTraced(id, seq)) {
        		return status(CONFLICT).body("seq="+seq+" has already been traced");
        	}
            return service.addTraces(id, seq, atm, end, traces)
                    ? accepted().build()
                    : internalServerError()
        			.header(TRACE_RETRY_HEADER, DO_RETRY)
        			.body("dispatch.state="+service.getDispatcherState());
        } catch (Exception e) {
            log.error("failed to add traces for instanceId=" + id + ", seq=" + seq, e);
            var retry = e instanceof DispatchProcessingException dpe && dpe.isRetryable()
            		? DO_RETRY 
            		: DO_NOT_RETRY;
            return internalServerError().header(TRACE_RETRY_HEADER, retry).body(e.getMessage());
        }
    }

    @GetMapping("queue")
    public List<EventTrace> peekQueue(){
        return service.peekQueue();
    }

    @PostMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
        service.updateState(state);
    }
}
