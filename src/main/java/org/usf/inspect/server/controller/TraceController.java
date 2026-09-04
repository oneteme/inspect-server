package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.exception.DispatchProcessingException;
import org.usf.inspect.server.service.TraceService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.usf.inspect.core.StatefulExecutionListener.SERVER_ERROR;
import static org.usf.inspect.core.StatefulExecutionListener.SUCCESS;
import static org.usf.inspect.server.Utils.isUUID;
import static org.usf.jquery.core.Utils.isEmpty;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
public class TraceController {

    private final TraceService service;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
            @RequestBody InstanceEnvironment instance){
        if(isEmpty(instance.getName())) {
            return status(BAD_REQUEST).body("invalid instance.name="+instance.getName());
        }
        if(!isUUID(String.valueOf(instance.getId()))) {
            return status(BAD_REQUEST).body("invalid instance.id="+instance.getId());
        }
        try {
            return service.addInstance(instance) //configure env<>namespace mapping
                    ? ok(instance.getId().toString())
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
        UUID instanceId;
        try {
            instanceId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return status(BAD_REQUEST).body("invalid instance ID");
        }
        try {
            for (var t : traces) {
                if (t instanceof AbstractRequestUpdate req && req.getStatus() < 0) {
                    if (req instanceof MailRequestUpdate mailReq && mailReq.isFailed()) {
                        mailReq.setStatus(SERVER_ERROR);
                    }
                    else if (req instanceof FtpRequestUpdate ftpReq && ftpReq.isFailed()) {
                        ftpReq.setStatus(SERVER_ERROR);
                    }
                    else if (req instanceof DatabaseRequestUpdate dbReq && dbReq.isFailed()) {
                        dbReq.setStatus(SERVER_ERROR);
                    }
                    else if (req instanceof DirectoryRequestUpdate dirReq && dirReq.isFailed()) {
                        dirReq.setStatus(SERVER_ERROR);
                    }
                    else {
                        req.setStatus(SUCCESS);
                    }
                }
            }
            return service.addTraces(traces, instanceId, attempts, filename, end)
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