package org.usf.trace.api.server.controller.v3;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.model.filter.JqueryRequestSessionFilter;
import org.usf.trace.api.server.model.wrapper.DatabaseRequestWrapper;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.trace.api.server.service.SessionQueueService;
import org.usf.trace.api.server.service.v3.JqueryV3RequestService;
import org.usf.traceapi.core.InstanceType;
import org.usf.traceapi.core.RestRequest;
import org.usf.traceapi.core.Session;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.trace.api.server.Utils.requireSingle;
import static org.usf.trace.api.server.config.TraceApiColumn.ID;
import static org.usf.trace.api.server.config.TraceApiTable.*;
import static org.usf.traceapi.core.Session.nextId;

@Slf4j
@CrossOrigin
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
public class ApiV3Controller {

    private final JqueryV3RequestService jqueryRequestService;
    private final SessionQueueService queueService;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody InstanceEnvironmentWrapper instance) {
        if(instance.getType() == InstanceType.CLIENT) {
            instance = instance.withAddress(hsr.getRemoteAddr());
        }
        instance.setInstanceId(nextId());
        return queueService.add(instance)
                ? accepted().body(instance.getInstanceId())
                : status(SERVICE_UNAVAILABLE).build();
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(@PathVariable String id,
                                            @RequestBody InstanceSession[] sessions) {
        for(InstanceSession s : sessions) {
            if(isNull(s.getId())) {
                if(s instanceof InstanceMainSession) {
                    s.setId(nextId()); // safe id set for web collectors
                }
                else if(s instanceof InstanceRestSession) {
                    log.warn("ApiSesstion id is null : {}", s);
                }
            }
            s.setInstanceId(id);
        }
        return queueService.add(sessions)
                ? accepted().build()
                : status(SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("session/api")
    public List<Session> getApiSessionsByCriteria(
            @RequestParam(required = false, name = "method") String[] methods,
            @RequestParam(required = false, name = "protocol") String[] protocols,
            @RequestParam(required = false, name = "host") String[] hosts,
            @RequestParam(required = false, name = "port") String[] ports,
            @RequestParam(required = false, name = "path") String path,
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(required = false, name = "media") String[] medias,
            @RequestParam(required = false, name = "auth") String[] auths,
            @RequestParam(required = false, name = "status") String[] status,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "apiname") String[] apiNames,
            @RequestParam(required = false, name = "user") String[] users,
            @RequestParam(required = false, name = "appname") String[] appNames,
            @RequestParam(required = false, name = "env") String[] environments){

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query);
        return jqueryRequestService.getApiSesssionsByCriteria(jsf, RestRequest::new,false);
    }

    @GetMapping("session/api/{id}")
    public ResponseEntity<Session> getIncomingRequestById(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(jqueryRequestService.getApiSessionById(Collections.singletonList(id), RestRequest::new, false)));
    }

    @GetMapping("session/api/{id}/instance")
    public ResponseEntity<InstanceEnvironmentWrapper> getInstanceByApiSessionId(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getInstanceBySessionId(id, APISESSION));
    }

    @GetMapping("session/api/{id}/parent")
    public ResponseEntity<Map<String,String>> getParentIdByChildId(@PathVariable String id){
        return Optional.of(jqueryRequestService.getSessionParent(id))
                .filter(o -> !o.isEmpty())
                .map(o -> ResponseEntity.ok().body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id}/tree")
    public Session getTreebyId(@PathVariable String id){
        return jqueryRequestService.getTreeById(id);
    }

    @GetMapping("session/db/{id}")
    public ResponseEntity<DatabaseRequestWrapper> getDatabaseRequestById(@PathVariable long id){
        return Optional.ofNullable(requireSingle(jqueryRequestService.getDatabaseRequests(DBQUERY.column(ID).equal(id),true)))
                .map(object -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1,TimeUnit.DAYS))
                        .body(object))
                .orElseGet(() ->ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noCache()).body(null));
    }

    @GetMapping("session/main")
    public List<Session> getMainRequestByCriteria(
            @RequestParam(required = false, name = "env") String[] environments,
            @RequestParam(required = false, name = "name") String[] names,
            @RequestParam(required = false, name = "launchmode") String[] launchModes,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "user") String[] users
    ) {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, null, environments, users, start, end, names, launchModes, location);
        return jqueryRequestService.getMainSessionsByCriteria(fc, RestRequest::new,false);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(jqueryRequestService.getMainSessionById(id, RestRequest::new, false)));
    }

    @GetMapping("session/main/{id}/instance")
    public ResponseEntity<InstanceEnvironmentWrapper> getInstanceByMainSessionId(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getInstanceBySessionId(id, MAINSESSION));
    }
}
