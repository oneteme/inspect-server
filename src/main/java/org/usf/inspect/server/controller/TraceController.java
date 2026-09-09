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
import static org.usf.inspect.core.DualEventTracer.SERVER_ERROR;
import static org.usf.inspect.core.DualEventTracer.SUCCESS;
import static org.usf.inspect.server.Utils.isUUID;
import static org.usf.jquery.core.Utils.isEmpty;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
public class TraceController{


    private final TraceService service;
    private final TraceV5Controller controller;


    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> addInstanceEnvironment(
            @RequestBody InstanceEnvironment instance){
       return controller.addInstanceEnvironment(instance, null);
    }


    @PutMapping(value = "instance/{id}/session", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addSessions(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces){
        UUID instanceId;
        try {
            instanceId = id;
        } catch (IllegalArgumentException e) {
            return status(BAD_REQUEST).body("invalid instance ID");
        }
        try {
            for (var t : traces) {
                if (t instanceof AbstractRequestUpdate req ) {
                    if (req instanceof MailRequestUpdate mailReq ) {
                        mailReq.setStatus(mailReq.isFailed() ? SERVER_ERROR : SUCCESS);

                    }
                    else if (req instanceof FtpRequestUpdate ftpReq ) {
                        ftpReq.setStatus(ftpReq.isFailed() ? SERVER_ERROR : SUCCESS);
                        }
                    else if (req instanceof DatabaseRequestUpdate dbReq ) {
                        dbReq.setStatus(dbReq.isFailed() ? SERVER_ERROR : SUCCESS);
                    }
                    else if (req instanceof DirectoryRequestUpdate dirReq ) {
                        dirReq.setStatus(dirReq.isFailed() ? SERVER_ERROR : SUCCESS);
                    }
                    else if (req instanceof LocalRequestUpdate localReq ) {
                        localReq.setStatus(localReq.getException() != null ? SERVER_ERROR : SUCCESS);
                    }
                }
            }
            return service.addTraces(instanceId, 1, attempts, end, traces)
                    ? accepted().build()
                    : status(SERVICE_UNAVAILABLE).body(new TraceFail(true, service.getDispatcherState().toString()));
        } catch (DispatchProcessingException e) {
            log.error("put sessions", e);
            return internalServerError().body(new TraceFail(e.isRetryable(), service.getDispatcherState().toString()));
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