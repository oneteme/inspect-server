package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.model.Architecture;
import org.usf.inspect.server.model.ServerInstanceEnvironment;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.object.*;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.jquery.core.Utils.isBlank;
import static org.usf.jquery.core.Utils.isEmpty;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private final RequestService requestService;
    private final SessionQueueService queueService;
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody InstanceEnvironment instance) {
    	if(isNull(instance) || isBlank(instance.getName())) {
    		return status(BAD_REQUEST).build();
    	}
        if(instance.getType() == CLIENT) {
            instance = instance.withAddress(hsr.getRemoteAddr());
        }
        instance.setId(nextId());
        try {
            requestService.addInstance(instance);
            return ok(instance.getId());
        } catch(Exception e) {
        	log.error("trace instance", e);
    		return internalServerError().build();
        }
    }

    @GetMapping("instance/{id}")
    public ResponseEntity<ServerInstanceEnvironment> getInstance(@PathVariable String id) throws SQLException {
        return ok().cacheControl(maxAge(1, DAYS)).body(requestService.getInstance(id));
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(@PathVariable String id, @RequestBody Session[] sessions) {
    	if(isEmpty(sessions)) {
    		return status(BAD_REQUEST).build();
    	}
    	try {
	        for(Session s : sessions) {
	            s.setInstanceId(id);
	            if(isNull(s.getId())) {
	                if(s instanceof MainSession) {
	                    s.setId(nextId()); // safe id set for web collectors
	                }
	                else if(s instanceof RestSession) {
	                    log.warn("RestSesstion.id is null : {}", s);
	                }
	            }
	        }
	        return (queueService.addSessions(sessions) ? accepted() : status(SERVICE_UNAVAILABLE)).build();
    	}
    	catch (Exception e) {
        	log.error("trace session", e);
    		return internalServerError().build();
    	}
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
            @RequestParam(required = false, name = "status") Integer[] status,
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "apiname") String[] apiNames,
            @RequestParam(required = false, name = "user") String[] users,
            @RequestParam(required = false, name = "appname") String[] appNames,
            @RequestParam(required = false, name = "env") String[] environments) throws SQLException {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query);
        return requestService.getRestSessionsForSearch(jsf);
    }

    @GetMapping("session/rest/{id}")
    public ResponseEntity<Session> getRestSession(@PathVariable String id) throws SQLException {
        return Optional.ofNullable(requestService.getRestSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/parent")
    public ResponseEntity<Map<String, String>> getSessionParent(@PathVariable String id) throws SQLException {
        return Optional.of(requestService.getSessionParent(id))
                .filter(o -> !o.isEmpty())
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{id}/tree")
    public ResponseEntity<Session> getMainTree(@PathVariable String id) throws SQLException {
        return Optional.ofNullable(requestService.getMainTree(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/tree")
    public ResponseEntity<Session> getRestTree(@PathVariable String id) throws SQLException {
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
            @RequestParam(required = false, name = "user") String[] users,
            @RequestParam(required = false, name = "appname") String[] appNames
    ) throws SQLException {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, appNames, environments, users, start, end, names, launchModes, location);
        return requestService.getMainSessionsForSearch(fc);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainSession(@PathVariable String id) throws SQLException { // without tree
        return Optional.ofNullable(requestService.getMainSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/rest")
    public ResponseEntity<List<RestRequest>> getRestRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getRestRequests(idSession));
    }

    @GetMapping("session/request/rest/exception")
    public ResponseEntity<Map<Long, ExceptionInfo>> getRestRequestExceptions(@RequestParam(required = true, name = "ids") Long[] idRequestList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getRestRequestExceptions(idRequestList));
    }

    @GetMapping("session/{id_session}/request/local")
    public ResponseEntity<List<LocalRequest>> getLocalRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLocalRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/database")
    public ResponseEntity<List<DatabaseRequest>> getDatabaseRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}")
    public ResponseEntity<DatabaseRequest> getDatabaseRequest(@PathVariable(name = "id_session") String idSession,
                                                                     @PathVariable(name = "id_database") long idDatabase) throws SQLException {
        return Optional.ofNullable(requestService.getDatabaseRequest(idDatabase))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}/stage")
    public ResponseEntity<List<DatabaseRequestStage>> getDatabaseRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                               @PathVariable(name = "id_database") long idDatabase) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequestStages(idDatabase));
    }

    @GetMapping("session/request/database/stages/count")
    public ResponseEntity<Map<Long,Integer>> getDatabaseRequestStagesRowCount(@RequestParam(required = true, name = "ids") Long[] idDatabaseList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getDatabaseRequestStageRowCount(idDatabaseList));
    }

    @GetMapping("session/request/database/exception")
    public ResponseEntity<Map<Long, ExceptionInfo>> getDatabaseRequestExceptions(@RequestParam(required = true, name = "ids") Long[] idDatabaseList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getDatabaseRequestExceptions(idDatabaseList));
    }

    @GetMapping("session/{id_session}/request/ftp")
    public ResponseEntity<List<FtpRequest>> getFtpRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getFtpRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}")
    public ResponseEntity<FtpRequest> getFtpRequest(@PathVariable(name = "id_session") String idSession,
                                                    @PathVariable(name = "id_ftp") long idFtp) throws SQLException {
        return Optional.ofNullable(requestService.getFtpRequest(idFtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}/stage")
    public ResponseEntity<List<FtpRequestStage>> getFtpRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                            @PathVariable(name = "id_ftp") long idFtp) throws SQLException {
        return Optional.ofNullable(requestService.getFtpRequestStages(idFtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/request/ftp/stages")
    public ResponseEntity<Map<Long,List<String>> > getFtpRequestStages(@RequestParam(required = true, name = "ids") Long[] idFtpList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getFtpRequestStages(idFtpList));
    }

    @GetMapping("session/request/ftp/exception")
    public ResponseEntity<Map<Long, ExceptionInfo>> getFtpRequestExceptions(@RequestParam(required = true, name = "ids") Long[] idFtpList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getFtpRequestExceptions(idFtpList));
    }
    @GetMapping("session/{id_session}/request/smtp")
    public ResponseEntity<List<MailRequest>> getSmtpRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getSmtpRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}")
    public ResponseEntity<MailRequest> getSmtpRequest(@PathVariable(name = "id_session") String idSession,
                                                             @PathVariable(name = "id_smtp") long idSmtp) throws SQLException {
        return Optional.ofNullable(requestService.getSmtpRequest(idSmtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}/stage")
    public ResponseEntity<List<MailRequestStage>> getSmtpRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                              @PathVariable(name = "id_smtp") long idSmtp) throws SQLException {
        return Optional.ofNullable(requestService.getSmtpRequestStages(idSmtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}/mail")
    public ResponseEntity<List<Mail>> getSmtpRequestMails(@PathVariable(name = "id_session") String idSession,
                                                          @PathVariable(name = "id_smtp") long idSmtp) throws SQLException {
        return Optional.ofNullable(requestService.getSmtpRequestMails(idSmtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/request/smtp/stages")
    public ResponseEntity<Map<Long,List<String>> > getSmtpRequestStages(@RequestParam(required = true, name = "ids") Long[] idFtpList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getSmtpRequestStages(idFtpList));
    }

    @GetMapping("session/request/smtp/stages/count")
    public ResponseEntity<Map<Long,Integer> > getSmtpRequestStagesRowCount(@RequestParam(required = true, name = "ids") Long[] idDatabaseList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getSmtpRequestStageRowCount(idDatabaseList));
    }

    @GetMapping("session/request/smtp/exception")
    public ResponseEntity<Map<Long, ExceptionInfo>> getSmtpRequestExceptions(@RequestParam(required = true, name = "ids") Long[] idSmtpList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getSmtpRequestExceptions(idSmtpList));
    }

    @GetMapping("session/{id_session}/request/ldap")
    public ResponseEntity<List<NamingRequest>> getLdapRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLdapRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/ldap/{id_ldap}")
    public ResponseEntity<NamingRequest> getLdapRequest(@PathVariable(name = "id_session") String idSession,
                                                               @PathVariable(name = "id_ldap") long idLdap) throws SQLException {
        return Optional.ofNullable(requestService.getLdapRequest(idLdap))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/ldap/{id_ldap}/stage")
    public ResponseEntity<List<NamingRequestStage>> getLdapRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                         @PathVariable(name = "id_ldap") long idLdap) throws SQLException {
        return Optional.ofNullable(requestService.getLdapRequestStages(idLdap))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/request/ldap/stages")
    public ResponseEntity<Map<Long,List<String>> > getLdapRequestStages(@RequestParam(required = true, name = "ids") Long[] idFtpList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getLdapRequestStages(idFtpList));
    }

    @GetMapping("session/request/ldap/exception")
    public ResponseEntity<Map<Long, ExceptionInfo>> getLdapRequestExceptions(@RequestParam(required = true, name = "ids") Long[] idLdapList) throws SQLException {
        return ResponseEntity.ok().body(requestService.getLdapRequestExceptions(idLdapList));
    }

    @GetMapping("architecture")
    public List<Architecture> getArchitecture(
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "env") String[] environments
    ) throws SQLException {
        return requestService.createArchitecture(start, end, environments);
    }
}
