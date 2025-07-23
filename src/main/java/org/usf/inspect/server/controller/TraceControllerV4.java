package org.usf.inspect.server.controller;

import static java.time.Instant.now;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.EventTraceScheduledDispatcher;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.service.RequestService;

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
                                             @RequestParam(required = false, name = "pending")  Integer pending,
                                             @RequestParam(required = false, name = "attempts") int attemps,
                                             @RequestParam(required = false, name = "end") Instant end
    ) {
        try {
            if(end != null){
                requestService.updateInstance(end, id);
            }
            executor.submit(()-> requestService.addInstanceTrace(new InstanceTrace(pending, attemps, sessions.length, now(), id))); //now !!!???
            if(dispatcher.emitAll(sessions)) {
            	return accepted().build();
            }
        }
        catch (Exception e) {
            log.error("trace session", e);
        }
        return internalServerError().build();
    }
}
