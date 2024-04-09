package org.usf.trace.api.server.controller;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.trace.api.server.Utils.requireSingle;
import static org.usf.traceapi.core.Session.nextId;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.trace.api.server.FilterCriteria;
import org.usf.trace.api.server.RequestDao;
import org.usf.trace.api.server.SessionQueueService;
import org.usf.traceapi.core.ApiRequest;
import org.usf.traceapi.core.ApiSession;
import org.usf.traceapi.core.ApplicationInfo;
import org.usf.traceapi.core.MainSession;
import org.usf.traceapi.core.Session;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {
	
    private final RequestDao dao;
    private final SessionQueueService queueService;

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

    @GetMapping("session/request")
    public List<Session> getIncomingRequestByCriteria(
    		@RequestParam(required = false, name = "name") String[] name,
    		@RequestParam(required = false, name = "env") String[] env,
    		@RequestParam(required = false, name = "port") String[] port,
    		@RequestParam(required = false, name = "start") Instant start,
    		@RequestParam(required = false, name = "end") Instant end,
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy){ // without tree
        FilterCriteria fc = new FilterCriteria(null,name,env,port,null,start,end);
        return dao.getIncomingRequestByCriteria(lazy, fc, ApiRequest::new);
    }

    @GetMapping("session/request/{id}")
    public ResponseEntity<Session> getIncomingRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok()
        		.cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
        		.body(requireSingle(dao.getIncomingRequestById(true, ApiRequest::new, id)));
    }

    @GetMapping("session/main")
    public List<MainSession> getMainRequestByCriteria(
            @RequestParam(required = false, name = "env") String[] env,
            @RequestParam(required = false, name = "launchmode") String[] launchMode,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy) {

        FilterCriteria fc = new FilterCriteria(null,null,env,null,launchMode,start,end);
        return dao.getMainRequestByCriteria(lazy, fc, ApiRequest::new);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<MainSession> getMainRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(dao.getMainRequestById(true, ApiRequest::new, id)));
    }

    @GetMapping("session/request/{id}/out")
    public ApiRequest getOutcomingRequestById(@PathVariable String id) {
        return dao.getOutcomingRequestById(id);
    }

    @GetMapping("session/request/{id}/tree")
    public Session getTreebyId(@PathVariable String id){
        return dao.getTreebyId(id);
    }
}


