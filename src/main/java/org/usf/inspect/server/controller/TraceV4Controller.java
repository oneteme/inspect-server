package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.service.TraceService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<Object> addInstanceEnvironment(
            @RequestBody InstanceEnvironment instance){
       return controller.addInstanceEnvironment(instance, null); //auto retention conversion
    }

    @PutMapping(value = "instance/{id}/session", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> addSessions(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces){
        var extractedExceptions = new ArrayList<EventTrace>();
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
            if (t instanceof AbstractStage stg && stg.getException() != null) {
                var ex = stg.getException();
                ex.setTraceId(stg.getRequestId());
                ex.setOffset(stg.getOrder());
                extractedExceptions.add(ex);
            } else if (t instanceof AbstractSessionUpdate ses && ses.getException() != null) {
                var ex = ses.getException();
                ex.setTraceId(ses.getId());
                ex.setOffset(nonNull(ses.getEnd()) ? end.toEpochMilli() : 1);
                extractedExceptions.add(ex);
            }else if (t instanceof LocalRequestUpdate req && req.getException() != null) {
                var ex = req.getException();
                ex.setTraceId(req.getId());
                ex.setOffset(nonNull(req.getEnd()) ? end.toEpochMilli() : 1);
                extractedExceptions.add(ex);
            }
        }
        if (!extractedExceptions.isEmpty()) {
            traces.addAll(extractedExceptions);
        }
        return controller.addTraces(id, 1, attempts, end, traces); //TODO check seq value
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