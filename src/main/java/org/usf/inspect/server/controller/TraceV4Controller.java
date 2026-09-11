package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.core.LogEntry.Level;
import org.usf.inspect.server.service.TraceService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.usf.inspect.core.DualEventTracer.SERVER_ERROR;
import static org.usf.inspect.core.DualEventTracer.SUCCESS;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
public class TraceV4Controller {

    private final TraceService service;
    private final TraceController controller;

    @Value("${inspect.server.migration.namespacePrefix}")
    private String namespacePrefix;


    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> addInstanceEnvironment(
            @RequestBody InstanceEnvironment instance){
        //Rétrocompatibilité namespace //TODO to English 
        if (instance != null && instance.getEnv() != null) {
            instance.setNamespace((namespacePrefix +"-"+ instance.getEnv()).toUpperCase());
        }
       return controller.addInstanceEnvironment(instance, null); //auto retention conversion
    }

    @PutMapping(value = "instance/{id}/session", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addSessions(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces){
        var addTraces = new ArrayList<EventTrace>();
        for (var t : traces) {
            resolveTraceUpdateStatus(t);
            resolveStagePayload(t);
            detachException(t, addTraces::add);
            convertLogEntry(t, addTraces::add);
        }
        if (!addTraces.isEmpty()) {
            traces.addAll(addTraces);
        }
        return controller.addTraces(id, 1, attempts, end, traces); //TODO check seq value
    }
    
    static void resolveTraceUpdateStatus(EventTrace trc) {
    	if (trc instanceof MailRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? SERVER_ERROR : SUCCESS);
        }
        else if (trc instanceof FtpRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? SERVER_ERROR : SUCCESS);
        }
        else if (trc instanceof DatabaseRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? SERVER_ERROR : SUCCESS);
        }
        else if (trc instanceof DirectoryRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? SERVER_ERROR : SUCCESS);
        }
        else if (trc instanceof LocalRequestUpdate localReq ) {
            localReq.setStatus(localReq.getException() != null ? SERVER_ERROR : SUCCESS);
        }
    }
    
    static void resolveStagePayload(EventTrace trc) {
        if (trc instanceof DatabaseRequestStage stg) {
            if(stg.getArgs() != null || stg.getCount() != null) {
            	stg.setPayload(new StagePayload(stg.getArgs(), stg.getCount()));
            }
        }
        else if (trc instanceof DirectoryRequestStage stg && stg.getArgs() != null) {
            stg.setPayload(new StagePayload(stg.getArgs(), null));
        }
        else if (trc instanceof FtpRequestStage stg && stg.getArgs() != null) {
            stg.setPayload(new StagePayload(stg.getArgs(), null));
        }
    }
    
    static void detachException(EventTrace trc, Consumer<ExceptionTrace> acc) {
        if (trc instanceof AbstractStage stg && stg.getException() != null) {
            var ex = stg.getException();
            ex.setTraceId(stg.getRequestId());
            ex.setOffset(stg.getOrder());
            acc.accept(ex);
        } else if (trc instanceof AbstractSessionUpdate upd && upd.getException() != null) {
            var ex = upd.getException();
            ex.setTraceId(upd.getId());
            ex.setOffset(nonNull(upd.getEnd()) ? upd.getEnd().toEpochMilli() : 1); //negative offset !!
            acc.accept(ex);
        }else if (trc instanceof LocalRequestUpdate upd && upd.getException() != null) {
            var ex = upd.getException();
            ex.setTraceId(upd.getId());
            ex.setOffset(nonNull(upd.getEnd()) ? upd.getEnd().toEpochMilli() : 1); //negative offset !!
            acc.accept(ex);
        }
    }
    
    static void convertLogEntry(EventTrace trc, Consumer<SessionEvent> acc) {
    	if(trc instanceof LogEntry log && log.getSessionId() != null && log.getLevel() != Level.REPORT) {
    		var evt = new SessionEvent(log.getInstant(), log.getLevel().name(), log.getMessage(), null, log.getSessionId());
    		acc.accept(evt);
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