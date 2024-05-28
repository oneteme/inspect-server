package org.usf.trace.api.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.trace.api.server.dao.RequestDao;
import org.usf.trace.api.server.model.wrapper.DatabaseRequestWrapper;
import org.usf.trace.api.server.service.SessionQueueService;
import org.usf.trace.api.server.service.JqueryRequestService;
import org.usf.trace.api.server.model.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.model.filter.JqueryRequestSessionFilter;
import org.usf.traceapi.core.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.trace.api.server.Utils.requireSingle;
import static org.usf.trace.api.server.config.TraceApiColumn.ID;
import static org.usf.trace.api.server.config.TraceApiTable.DBQUERY;
import static org.usf.traceapi.core.Session.nextId;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
public class ApiController {
	
    private final RequestDao dao;
    private final JqueryRequestService jqueryRequestService;
    private final SessionQueueService queueService;

    public ApiController(RequestDao dao, JqueryRequestService jqueryRequestService, SessionQueueService queueService) {
        this.dao = dao;
        this.jqueryRequestService = jqueryRequestService;
        this.queueService = queueService;
    }

    @PutMapping("session")
    public ResponseEntity<Void> saveSession(HttpServletRequest hsr,@RequestBody Session[] sessions) {
        for(Session s : sessions) {
            if(isNull(s.getId())) {
                if(s instanceof MainSession ms) {
                    s.setId(nextId()); // safe id set for web collectors
                    updateRemoteAddress(hsr, ms);
                }
                else if(s instanceof ApiSession) {
                    log.warn("ApiSesstion id is null : {}", s);
                }
            }
        }
        return queueService.add(sessions) 
        		? accepted().build()
        		: status(SERVICE_UNAVAILABLE).build();
    }
    public void  updateRemoteAddress(HttpServletRequest hsr,  @RequestBody MainSession req) {
    	if(isNull(req.getApplication())) { //set IP address for WABAPP trace
    		req.setApplication(new ApplicationInfo(null, null, hsr.getRemoteAddr(), null, null, null));
    	}
    	else if(isNull(req.getApplication().getAddress())) {
    		req.setApplication(req.getApplication().withAddress(hsr.getRemoteAddr()));
    	}
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
    		@RequestParam(required = false, name = "env") String[] environments,
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy){ // without tree
            JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, start, end, lazy, methods, protocols, hosts, ports, medias, auths, status, apiNames, users, path, query);
        return jqueryRequestService.getApiSesssionsByCriteria(jsf, ApiRequest::new,false);
    }

    @GetMapping("session/api/{id}")
    public ResponseEntity<Session> getIncomingRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(jqueryRequestService.getApiSessionById(Collections.singletonList(id), ApiRequest::new, false)));
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
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy) {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, null, environments, start, end, lazy, names, launchModes, location);
        return jqueryRequestService.getMainSessionsByCriteria(fc, ApiRequest::new,false);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(jqueryRequestService.getMainSessionById(id, ApiRequest::new, false)));
    }



}


