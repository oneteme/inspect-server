package org.usf.trace.api.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.trace.api.server.FilterCriteria;
import org.usf.trace.api.server.RequestDao;
import org.usf.trace.api.server.SessionQueueService;
import org.usf.traceapi.core.*;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.usf.trace.api.server.Utils.requireSingle;
import static org.usf.traceapi.core.Session.nextId;

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
                if(s instanceof MainSession) {
                    s.setId(nextId()); // safe id set for web collectors
                    updateRemoteAddress(hsr,(MainSession) s);
                }
                else if(s instanceof ApiSession) {
                    log.warn("ApiSesstion id is null : {}", s);
                }
            }
        }
        queueService.add(sessions);
        return accepted().build();
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
            @RequestParam(required = false, name = "method") String[] method,
            @RequestParam(required = false, name = "protocol") String[] protocol,
            @RequestParam(required = false, name = "host") String[] host,
            @RequestParam(required = false, name = "port") String[] port,
            @RequestParam(required = false, name = "path") String path,
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(required = false, name = "media") String[] media,
            @RequestParam(required = false, name = "auth") String[] auth,
            @RequestParam(required = false, name = "status") String[] status,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "apiname") String[] apiname,
            @RequestParam(required = false, name = "user") String[] user,
    		@RequestParam(required = false, name = "appname") String[] appname,
    		@RequestParam(required = false, name = "env") String[] env,


            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy){ // without tree
        FilterCriteria fc = new FilterCriteria(null,method,protocol,host,port,path,query,media,auth,status,start,end,apiname,user,appname,env,null,null, null);
        return dao.getIncomingRequestByCriteria(lazy, fc, ApiRequest::new);
    }

    @GetMapping("session/request/{id}")
    public ResponseEntity<Session> getIncomingRequestById(@PathVariable String id) { // without tree
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requireSingle(dao.getIncomingRequestById(true, ApiRequest::new, id)));
    }

    @GetMapping("session/main")
    public List<MainSession> getMainRequestByCriteria(
            @RequestParam(required = false, name = "env") String[] env,
            @RequestParam(required = false, name = "name") String[] name,
            @RequestParam(required = false, name = "launchmode") String[] launchMode,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(defaultValue = "true", name = "lazy") boolean lazy) {

        FilterCriteria fc = new FilterCriteria(null,null,null,null,null,null,null,null,null,null, start, end,null,null,null,env,launchMode,location,name);
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


