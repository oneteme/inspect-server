package org.usf.inspect.server.controller;

import static java.util.Objects.nonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.usf.inspect.core.DualEventTracer.APP_ERROR;
import static org.usf.inspect.core.DualEventTracer.INT_ERROR;
import static org.usf.inspect.core.DualEventTracer.SUCCESS;
import static org.usf.inspect.core.InspectLogger.LogLevel.REPORT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
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
import org.usf.inspect.core.AbstractSessionUpdate;
import org.usf.inspect.core.AbstractStage;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.DatabaseRequestUpdate;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.FtpRequestUpdate;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.LocalRequestUpdate;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.MainSessionUpdate;
import org.usf.inspect.core.ReportEvent;
import org.usf.inspect.core.SessionEvent;
import org.usf.inspect.core.StagePayload;
import org.usf.inspect.server.model.UserAction;
import org.usf.inspect.server.service.TraceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        //Backward compatibility for namespace
        if (instance != null && instance.getEnv() != null) {
            instance.setNamespace((namespacePrefix +"-"+ instance.getEnv()).toUpperCase());
        }
       return controller.addInstanceEnvironment(instance, 1, null); //auto retention conversion
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
            convertToSessionEvent(t, addTraces::add);
        }
        traces.removeIf(t-> t instanceof UserAction 
        		|| (t instanceof ReportEvent rpr && nonNull(rpr.getSessionId()) && !REPORT.name().equals(rpr.getLevel())));
        if (!addTraces.isEmpty()) {
            traces.addAll(addTraces);
        }
        return controller.addTraces(id, 1, attempts, end, traces); //TODO check seq value
    }
    
    static void resolveTraceUpdateStatus(EventTrace trc) {
    	if (trc instanceof LocalRequestUpdate upd ) {
            upd.setStatus(upd.getException() != null ? APP_ERROR : SUCCESS);
        }
    	if (trc instanceof MainSessionUpdate upd ) {
            upd.setStatus(upd.getException() != null ? APP_ERROR : SUCCESS);
        }
    	//else httpSessionUpdate has already a status set
		else if (trc instanceof DatabaseRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? INT_ERROR : SUCCESS);
        }
        else if (trc instanceof FtpRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? INT_ERROR : SUCCESS);
        }
        else if (trc instanceof MailRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? INT_ERROR : SUCCESS);
        }
        else if (trc instanceof DirectoryRequestUpdate upd ) {
            upd.setStatus(upd.isFailed() ? INT_ERROR : SUCCESS);
        }
    }
    
    static void resolveStagePayload(EventTrace trc) {
        if (trc instanceof DatabaseRequestStage stg) {
            if(stg.getArgs() != null || stg.getCount() != null) {
            	stg.setPayload(new StagePayload(stg.getArgs(), stg.getCount()));
            }
        }
        else if (trc instanceof DirectoryRequestStage stg) {
        	if(stg.getArgs() != null) {
        		stg.setPayload(new StagePayload(stg.getArgs(), null));
        	}
        }
        else if (trc instanceof FtpRequestStage stg && stg.getArgs() != null) {
            stg.setPayload(new StagePayload(stg.getArgs(), null));
        }
    }
    
    static void detachException(EventTrace trc, Consumer<ExceptionTrace> acc) {
        if (trc instanceof AbstractSessionUpdate upd) {
        	if(nonNull(upd.getException())) {
                var exp = upd.getException();
                exp.setTraceId(upd.getId());
                exp.setOffset(nonNull(upd.getEnd()) ? upd.getEnd().toEpochMilli() : 1);
                acc.accept(exp);
        	}
        }
        else if (trc instanceof LocalRequestUpdate upd) {
        	if(nonNull(upd.getException())) {
                var exp = upd.getException();
                exp.setTraceId(upd.getId());
                exp.setOffset(nonNull(upd.getEnd()) ? upd.getEnd().toEpochMilli() : 1);
                acc.accept(exp);
        	}
        }
        else if (trc instanceof AbstractStage stg && nonNull(stg.getException())) {
    		var exp = stg.getException();
    		exp.setTraceId(stg.getRequestId());
    		exp.setOffset(stg.getOrder());
    		acc.accept(exp);
        }
    }

    static void convertToSessionEvent(EventTrace trc, Consumer<SessionEvent> acc) {
    	if(trc instanceof ReportEvent rpr) {
    		if(nonNull(rpr.getSessionId()) && !REPORT.name().equals(rpr.getLevel())) {
    			var evt = new SessionEvent(rpr.getInstant(), rpr.getLevel(), rpr.getMessage(), rpr.getThread(), null, rpr.getSessionId());
    			acc.accept(evt);
    		}
    	}
    	else if(trc instanceof UserAction act) { //TODO check UserAction NamedType => JsonUtils
    		var evt = new SessionEvent(act.getStart(), act.getType(), act.getName(), null, act.getNodeName(), act.getCdSession());
    		acc.accept(evt);
    	}
    }

    @GetMapping(value = "queue", produces = APPLICATION_JSON_VALUE)
    public Collection<EventTrace> peekQueue(){
        return service.peekQueue();
    }

    @PostMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
        service.updateState(state);
    }
}
