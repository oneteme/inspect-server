package org.usf.inspect.server.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.Utils.isUUID;
import static org.usf.jquery.core.Utils.isEmpty;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

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
import org.usf.inspect.core.TraceFail;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.service.TraceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/v5/trace", produces = APPLICATION_JSON_VALUE)
public class TraceV5Controller {

    private final TraceService service;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(@RequestBody InstanceEnvironment instance, Principal principal){ //check
    	if(isEmpty(instance.getName())) {
    		return status(BAD_REQUEST).body("invalid instance.name="+instance.getName());
    	}
        if(!isUUID(instance.getId())) {
            return status(BAD_REQUEST).body("invalid instance.id="+instance.getId());
        }
		try {
			return service.addInstance(instance, principal.getName())
					? ok(instance.getId())
					: status(SERVICE_UNAVAILABLE).body("dispatcher.state=" + service.getState());
		} catch(Exception e) {
			log.error("post instance", e);
			return internalServerError().body("unexpected exception " + e.getClass().getSimpleName());
		}
    }

    @PutMapping(value = "instance/{id}/session", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addSessions(
    		@PathVariable String id,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces){
        if(!isUUID(id)) {
            return status(BAD_REQUEST).body("invalid instance ID");
        }
        try {
            return service.addTraces(traces, id, attempts, filename, end)
                    ? accepted().build()
                    : status(SERVICE_UNAVAILABLE).body(new TraceFail(service.getState().toString(), true));
        } catch (DispatchProcessingException e) {
            log.error("put sessions", e);
            return internalServerError().body(new TraceFail(service.getState().toString(), e.isRetryable()));
        }
    }
    
    @GetMapping(value = "queue", produces = APPLICATION_JSON_VALUE)
    public List<EventTrace> peekQueue(){
		return service.peekQueue();
    }

    @PostMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
		service.updateState(state);
    }
}
