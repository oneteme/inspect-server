package org.usf.inspect.server.controller;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.Session.nextId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.RestRequest;
import org.usf.inspect.core.Session;
import org.usf.inspect.server.model.InstanceMainSession;
import org.usf.inspect.server.model.InstanceRestSession;
import org.usf.inspect.server.model.InstanceSession;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
public class TraceController {

    private final  RequestService   requestService;
    private final SessionQueueService queueService;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody InstanceEnvironmentWrapper instance) {
        if(instance.getType() == InstanceType.CLIENT) {
            instance = instance.withAddress(hsr.getRemoteAddr());
        }
        instance.setId(nextId());
        try {
            requestService.addInstanceEnvironment(instance);
            return accepted().body(instance.getId());
        } catch(Exception e) {
            return status(SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("instance/{id}")
    public ResponseEntity<InstanceEnvironmentWrapper> getInstance(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getInstanceById(id));
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

    @GetMapping("session/rest")
    public List<Session> getRestSessions(
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
        return requestService.getRestSessions(jsf);
    }

    @GetMapping("session/rest/{id}")
    public ResponseEntity<Session> getRestSession(@PathVariable String id) {
        return Optional.ofNullable(requestService.getRestSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/parent")
    public ResponseEntity<Map<String, String>> getSessionParent(@PathVariable String id){
        return Optional.of(requestService.getSessionParent(id))
                .filter(o -> !o.isEmpty())
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{id}/tree")
    public ResponseEntity<Session> getMainTree(@PathVariable String id){
        return Optional.ofNullable(requestService.getMainTree(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/tree")
    public ResponseEntity<Session> getRestTree(@PathVariable String id){
        return Optional.ofNullable(requestService.getRestTree(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main")
    public List<Session> getMainSessions(
            @RequestParam(required = false, name = "env") String[] environments,
            @RequestParam(required = false, name = "name") String[] names,
            @RequestParam(required = false, name = "launchmode") String[] launchModes,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "user") String[] users
    ) {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, null, environments, users, start, end, names, launchModes, location);
        return requestService.getMainSessions(fc);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainSession(@PathVariable String id) { // without tree
        return Optional.ofNullable(requestService.getMainSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id}/request/rest")
    public ResponseEntity<List<RestRequestWrapper>> getRestRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getRestRequests(id, RestRequest::new));
    }

    @GetMapping("session/{id}/request/local")
    public ResponseEntity<List<LocalRequestWrapper>> getLocalRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLocalRequests(id));
    }

    @GetMapping("session/{id}/request/database")
    public ResponseEntity<List<DatabaseRequestWrapper>> getDatabaseRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequests(id));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}")
    public ResponseEntity<DatabaseRequestWrapper> getDatabaseRequest(@PathVariable(name = "id_session") String idSession,
                                                                     @PathVariable(name = "id_database") long idDatabase){
        return Optional.ofNullable(requestService.getDatabaseRequest(idDatabase))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}/stage")
    public ResponseEntity<List<DatabaseRequestStageWrapper>> getDatabaseRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                                @PathVariable(name = "id_database") long idDatabase){
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequestStages(idDatabase));
    }

}
