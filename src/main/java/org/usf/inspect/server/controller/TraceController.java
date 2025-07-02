package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;
import java.time.Instant;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.jquery.core.Utils.isBlank;
import static org.usf.jquery.core.Utils.isEmpty;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private final RequestService requestService;
    private final SessionQueueService queueService;
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody InstanceEnvironment instance) {
    	if(queueService.getState() == DISABLE) {
            return status(SERVICE_UNAVAILABLE).build();
        }
        if(isNull(instance) || isBlank(instance.getName())) {
    		return status(BAD_REQUEST).build();
    	}
        if(instance.getType() == CLIENT) {
            instance.setAddress(hsr.getRemoteAddr());
        }
        instance.setId(nextId());
        try {
            requestService.addInstance(instance);
            return ok(instance.getId());
        } catch(Exception e) {
        	log.error("trace instance", e);
    		return internalServerError().build();
        }
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions( @PathVariable("id") String id,
                                             @RequestBody Session[] sessions,
                                             @RequestParam(required = false, name = "pending")  Integer pending,
                                             @RequestParam(required = false, name = "end") Instant end
                                            ) {
    	if(isEmpty(sessions)) {
    		return status(BAD_REQUEST).build();
    	}
    	try {
            if(end != null){
                requestService.updateInstance(end,id);
                log.warn("Instance with id : {} has ended", id);
            }
            if(pending != null){
                log.info("Pending sessions : {}", pending);
            }
	        for(Session s : sessions) {
	            s.setInstanceId(id);
	            if(isNull(s.getId())) {
	                if(s instanceof MainSession) {
	                    s.setId(nextId()); // safe id set for web collectors
	                }
	                else if(s instanceof RestSession) {
	                    log.warn("RestSesstion.id is null : {}", s);
	                }
	            }
	        }
	        return (queueService.addSessions(sessions) ? accepted() : status(SERVICE_UNAVAILABLE)).build();
    	}
    	catch (Exception e) {
        	log.error("trace session", e);
    		return internalServerError().build();
    	}
    }
}
