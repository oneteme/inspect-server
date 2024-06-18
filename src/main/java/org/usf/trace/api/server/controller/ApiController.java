package org.usf.trace.api.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.trace.api.server.service.SessionQueueService;
import org.usf.trace.api.server.service.JqueryRequestService;
import org.usf.traceapi.core.*;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.traceapi.core.InstanceType.*;
import static org.usf.traceapi.core.Session.nextId;

@Deprecated
@Slf4j
@CrossOrigin
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
public class ApiController {

    private final JqueryRequestService jqueryRequestService;
    private final SessionQueueService queueService;

    private static Predicate<InstanceEnvironmentWrapper> mainPredicate(InstanceMainSession ms) {
        return c -> ms.getApplication() != null && c.getName().equals(ms.getApplication().getName())
                && c.getEnv().equals(ms.getApplication().getEnv()) && c.getUser().equals(ms.getApplication().getUser());
    }

    private static Predicate<InstanceEnvironmentWrapper> restPredicate(InstanceRestSession rs) {
        return c -> rs.getApplication() != null && c.getName().equals(rs.getApplication().getName())
                && c.getEnv().equals(rs.getApplication().getEnv());
    }

    @PutMapping("session")
    public ResponseEntity<Void> saveSession(HttpServletRequest hsr, @RequestBody InstanceSession[] sessions) {
        Stream<InstanceEnvironmentWrapper> instanceStream = jqueryRequestService.cache().stream();
        for(InstanceSession s : sessions) {
            if(s instanceof InstanceMainSession ms) {
                if(isNull(s.getId())) {
                    s.setId(nextId()); // safe id set for web collectors
                }
                Optional<InstanceEnvironmentWrapper> instance = instanceStream.filter(mainPredicate(ms)).findFirst();
                String nextId;
                if(instance.isPresent()) {
                    nextId = instance.get().getInstanceId();
                } else {
                    nextId = nextId();
                    var application = ms.getApplication() != null ?
                            new InstanceEnvironmentWrapper(nextId, ms.getApplication().getName(),
                                ms.getApplication().getVersion(), hsr.getRemoteAddr(), ms.getApplication().getEnv(),
                                ms.getApplication().getOs(), ms.getApplication().getRe(), ms.getApplication().getUser(), ms.getApplication().getType(),
                                ms.getApplication().getInstant(), ms.getApplication().getCollector()) :
                            new InstanceEnvironmentWrapper(nextId, null, null, hsr.getRemoteAddr(), null, null, null,null, CLIENT, Instant.now(),null);

                    try {
                        queueService.add(application);
                    } catch (Exception e) {
                        return status(SERVICE_UNAVAILABLE).build();
                    }
                }
                ms.setInstanceId(nextId);
            }
            else if(s instanceof InstanceRestSession rs) {
                if(isNull(s.getId())) {
                    log.warn("ApiSesstion id is null : {}", s);
                }
                Optional<InstanceEnvironmentWrapper> instance = instanceStream.filter(restPredicate(rs)).findFirst();
                instance.ifPresent(instanceEnvironmentWrapper -> rs.setInstanceId(instanceEnvironmentWrapper.getInstanceId()));
            }
        }
        return queueService.add(sessions) 
        		? accepted().build()
        		: status(SERVICE_UNAVAILABLE).build();
    }
}


