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
import org.usf.trace.api.server.model.wrapper.*;
import org.usf.trace.api.server.service.SessionQueueService;
import org.usf.trace.api.server.service.v3.JqueryV3RequestService;
import org.usf.traceapi.core.*;

import java.time.Instant;
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
        instance.setId(nextId());
        return queueService.add(instance)
                ? accepted().body(instance.getId())
                : status(SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("instance/{id}")
    public ResponseEntity<InstanceEnvironmentWrapper> getInstance(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getInstanceById(id));
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
        return jqueryRequestService.getRestSessions(jsf);
    }

    @GetMapping("session/rest/{id}")
    public ResponseEntity<Session> getRestSession(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getRestSession(id));
    }

    @GetMapping("session/rest/{id}/parent")
    public ResponseEntity<Map<String, String>> getParentIdByChildId(@PathVariable String id){
        return Optional.of(jqueryRequestService.getSessionParent(id))
                .filter(o -> !o.isEmpty())
                .map(o -> ResponseEntity.ok().body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{id}/tree")
    public Session getMainTreebyId(@PathVariable String id){
        return jqueryRequestService.getMainTreeById(id);
    }

    @GetMapping("session/rest/{id}/tree")
    public Session getRestTreebyId(@PathVariable String id){
        return jqueryRequestService.getRestTreeById(id);
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
        return jqueryRequestService.getMainSessions(fc);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainSession(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getMainSession(id));
    }

    @GetMapping("session/{id}/request/rest")
    public ResponseEntity<List<RestRequestWrapper>> getRestRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getRestRequests(id, RestRequest::new));
    }

    @GetMapping("session/{id}/request/runnable")
    public ResponseEntity<List<RunnableStageWrapper>> getRunnableRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getRunnableStages(id));
    }

    @GetMapping("session/{id}/request/database")
    public ResponseEntity<List<DatabaseRequestWrapper>> getDatabaseRequests(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getDatabaseRequests(id));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}")
    public ResponseEntity<DatabaseRequestWrapper> getDatabaseRequest(@PathVariable(name = "id_session") String idSession,
                                                                     @PathVariable(name = "id_database") long idDatabase){
        DatabaseRequestWrapper databaseRequest = jqueryRequestService.getDatabaseRequest(idDatabase);
        return databaseRequest != null ?
                ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(databaseRequest) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noCache()).body(null);
    }

    @GetMapping("session/{id_session}/request/database/{id_database}/action")
    public ResponseEntity<List<DatabaseRequestStage>> getDatabaseActions(@PathVariable(name = "id_session") String idSession,
                                                                         @PathVariable(name = "id_database") long idDatabase){
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(jqueryRequestService.getDatabaseActions(idDatabase));
    }

}
