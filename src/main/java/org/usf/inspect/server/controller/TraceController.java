package org.usf.inspect.server.controller;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.Session.nextId;
import static org.usf.jquery.core.Utils.isBlank;
import static org.usf.jquery.core.Utils.isEmpty;

import java.sql.SQLException;
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
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.*;
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
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private final RequestService requestService;
    private final SessionQueueService queueService;
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(HttpServletRequest hsr, @RequestBody ServerInstanceEnvironment instance) {
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
    public ResponseEntity<Void> addSessions(@PathVariable String id, @RequestBody ServerSession[] sessions) {
    	if(isEmpty(sessions)) {
    		return status(BAD_REQUEST).build();
    	}
    	try {
	        for(ServerSession s : sessions) {
	            s.setInstanceId(id);
	            if(isNull(s.getId())) {
	                if(s instanceof ServerMainSession) {
	                    s.setId(nextId()); // safe id set for web collectors
	                }
	                else if(s instanceof ServerRestSession) {
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
    public ResponseEntity<List<RestRequestWrapper>> getRestRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getRestRequests(idSession, RestRequest::new));
    }

    @GetMapping("session/{id_session}/request/local")
    public ResponseEntity<List<LocalRequestWrapper>> getLocalRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLocalRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/database")
    public ResponseEntity<List<DatabaseRequestWrapper>> getDatabaseRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}")
    public ResponseEntity<DatabaseRequestWrapper> getDatabaseRequest(@PathVariable(name = "id_session") String idSession,
                                                                     @PathVariable(name = "id_database") long idDatabase) throws SQLException {
        return Optional.ofNullable(requestService.getDatabaseRequest(idDatabase))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}/stage")
    public ResponseEntity<List<DatabaseRequestStageWrapper>> getDatabaseRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                                    @PathVariable(name = "id_database") long idDatabase) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequestStages(idDatabase));
    }

    @GetMapping("session/{id_session}/request/ftp")
    public ResponseEntity<List<FtpRequestWrapper>> getFtpRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getFtpRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}")
    public ResponseEntity<FtpRequestWrapper> getFtpRequest(@PathVariable(name = "id_session") String idSession,
                                                           @PathVariable(name = "id_ftp") long idFtp) throws SQLException {
        return Optional.ofNullable(requestService.getFtpRequest(idFtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}/stage")
    public ResponseEntity<List<FtpRequestStageWrapper>> getFtpRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                            @PathVariable(name = "id_ftp") long idFtp) throws SQLException {
        return Optional.ofNullable(requestService.getFtpRequestStages(idFtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/smtp")
    public ResponseEntity<List<MailRequestWrapper>> getSmtpRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getSmtpRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}")
    public ResponseEntity<MailRequestWrapper> getSmtpRequest(@PathVariable(name = "id_session") String idSession,
                                                             @PathVariable(name = "id_smtp") long idSmtp) throws SQLException {
        return Optional.ofNullable(requestService.getSmtpRequest(idSmtp))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}/stage")
    public ResponseEntity<List<MailRequestStageWrapper>> getSmtpRequestStages(@PathVariable(name = "id_session") String idSession,
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

    @GetMapping("session/{id_session}/request/ldap")
    public ResponseEntity<List<NamingRequestWrapper>> getLdapRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLdapRequests(idSession));
    }

    @GetMapping("session/{id_session}/request/ldap/{id_ldap}")
    public ResponseEntity<NamingRequestWrapper> getLdapRequest(@PathVariable(name = "id_session") String idSession,
                                                               @PathVariable(name = "id_ldap") long idLdap) throws SQLException {
        return Optional.ofNullable(requestService.getLdapRequest(idLdap))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/ldap/{id_ldap}/stage")
    public ResponseEntity<List<NamingRequestStageWrapper>> getLdapRequestStages(@PathVariable(name = "id_session") String idSession,
                                                                         @PathVariable(name = "id_ldap") long idLdap) throws SQLException {
        return Optional.ofNullable(requestService.getLdapRequestStages(idLdap))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
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
