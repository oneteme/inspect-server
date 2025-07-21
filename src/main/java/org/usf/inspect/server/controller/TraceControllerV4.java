package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceControllerV4 {

    private final RequestService requestService;
    private final SessionQueueService queueService;
    private final ExecutorService executor = Executors.newFixedThreadPool(15);

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions( @PathVariable("id") String id,
                                             @RequestBody EventTrace[] sessions,
                                             @RequestParam(required = false, name = "pending")  Integer pending,
                                             @RequestParam(required = false, name = "end") Instant end,
                                             @RequestParam(required = false, name = "attemps") int attemps
    ) {
        try {
            if(end != null){
                requestService.updateInstance(end, id);
            }
            executor.submit(()-> requestService.addInstanceTrace(new InstanceTrace(pending, attemps, sessions.length, Instant.now(),id)));
            //queueService.addEventTraces(sessions);
            return accepted().build();
        }
        catch (Exception e) {
            log.error("trace session", e);
            return internalServerError().build();
        }
    }
}
