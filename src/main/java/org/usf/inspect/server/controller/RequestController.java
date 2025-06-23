package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.server.config.TraceApiDatabase;
import org.usf.inspect.server.dto.DtoRequest;
import org.usf.inspect.server.dto.DtoRestRequest;
import org.usf.inspect.server.mapper.InspectMappers;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.service.AnalyticService;
import org.usf.inspect.server.service.RequestService;
import org.usf.jquery.core.Mappers;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.QueryRequestFilter;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final AnalyticService analyticService;


    @GetMapping("instance")
    public ResponseEntity<List<InstanceEnvironment>> getInstance(
       @QueryRequestFilter(view = "instance",
                           column = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,id") QueryComposer request) throws SQLException {
        return ok()
                .cacheControl(maxAge(1, DAYS))
                .body(INSPECT.execute(request, InspectMappers.instanceEnvironmentMapper()));
    }




  /*  @GetMapping("request/rest/{id}")
    public RestRequest getRestRequestById (@PathVariable int id) throws SQLException {
        return Optional.ofNullable(requestService.getRestRequestsCompleteById(id))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null)).getBody();
    }*/

//    @GetMapping("request/rest/{id}") //id_req=123 | prnt_cd = 123
//    public RestRequest getRestRequestById2 (
//            @QueryRequestFilter(
//            view = "ID",
//            column = "count",
//            join = "exception",
//            filters = {},
//            order = "start") QueryComposer query) throws SQLException {
//        return Optional.ofNullable(requestService.getRestRequestsCompleteById(id))
//                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
//                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null)).getBody();
//    }


    //@QueryRequestFilter(database = "acc", view = "ratt", column = "pom,filiere", join = "conv", ignoreParameters = { "start", "end" }) QueryComposer request)

    // todo :  not yet
    @GetMapping("request/{type}/hosts")
    public String[] getRequestsHostsbyType(
            @PathVariable String type,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end) throws SQLException {
        try {
            RequestType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return requestService.getRequestsHostsByType(type, environment, start, end);
    }

    /*@GetMapping("request/rest") // todo:  parameters : env, host, start, end, rangestatus
    public List<RestRequest> getRestRequests(
            @QueryRequestFilter(view = "rest_request",
                    column = "id,protocol,auth,host,port,path,query,method,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,remote,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start.asc") QueryComposer request) throws SQLException {
        return Optional.ofNullable(TraceApiDatabase.INSPECT.execute(request, InspectMappers.restRequestMapper()))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null)).getBody();
    }*/


    @GetMapping("request/rest")
    public List<DtoRestRequest> getRestRequests(@RequestParam(required = false, name = "env") String[] environments,
                                                @RequestParam(required = false, name = "host") String[] hosts,
                                                @RequestParam(required = false, name = "start") Instant start,
                                                @RequestParam(required = false, name = "end") Instant end,
                                                @RequestParam(required = false, name = "rangestatus") String [] rangestatus) throws SQLException {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, null, environments, null, start, end, null, null, hosts, null, null, null, null, null, null, null,rangestatus);
        return requestService.getRestRequestsLazyForSearch(jsf);
    }

    /*@GetMapping("request/database") // todo:  parameters : env, host, start, end, rangestatus
    public List<RestRequest> getDatabaseRequest(
            @QueryRequestFilter(view = "database_request",
                    column = "id,host,db,start,end,thread,command,status,schema,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start.asc") QueryComposer request) throws SQLException {
        return Optional.ofNullable(TraceApiDatabase.INSPECT.execute(request, InspectMappers.restRequestMapper()))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null)).getBody();
    }*/

    @GetMapping("request/database")
    public List<DtoRequest> getDatabaseRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                        @RequestParam(required = false, name = "host") String[] hosts,
                                                        @RequestParam(required = false, name = "start") Instant start,
                                                        @RequestParam(required = false, name = "end") Instant end,
                                                        @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus) throws SQLException {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getDatabaseRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/ftp")
    public List<DtoRequest> getFtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                   @RequestParam(required = false, name = "host") String[] hosts,
                                                   @RequestParam(required = false, name = "start") Instant start,
                                                   @RequestParam(required = false, name = "end") Instant end,
                                                   @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus) throws SQLException {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getFtpRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/smtp")
    public List<DtoRequest> getSmtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus) throws SQLException {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getSmtpRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/ldap")
    public List<DtoRequest> getLdapRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus) throws SQLException {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getLdapRequestsLazyForSearch(jsf);
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
            @RequestParam(required = false, name = "env") String[] environments,
            @RequestParam(required = false, name = "rangestatus") String [] rangestatus) throws SQLException {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query,rangestatus);
        return requestService.getRestSessionsForSearch(jsf);
    }

    @GetMapping("session/rest/{appName}/dump")
    public List<Session> getRestSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    ) throws SQLException {
        return requestService.getRestSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/rest/{id}")
    public List<Session> getRestSession(
            @QueryRequestFilter(view = "rest_session",
            column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,mask,user,user_agt,cache_control,instance_env",) QueryComposer request) throws SQLException{
        return INSPECT.execute(request, InspectMappers.restSessionWithInstanceMapper());
    }

    @GetMapping("session/rest/{id}/winstance")
    public List<Session> getRestSessionWithInstance(
            @QueryRequestFilter(view = "rest_session",
                    column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,mask,user,user_agt,cache_control,instance_env,app_name,os,re,address",
                    join="instance") QueryComposer request) throws SQLException{
        return INSPECT.execute(request, InspectMappers.restSessionWithoutInstanceMapper());
    }

    @GetMapping("session/rest/{id}") //todo : remove
    public ResponseEntity<Session> getRestSession(@PathVariable String id) throws SQLException {
        return Optional.ofNullable(requestService.getRestSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()->ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/parent") // can't optimise
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
            @RequestParam(required = false, name = "appname") String[] appNames,
            @RequestParam(required = false, name = "rangestatus") String [] rangestatus
    ) throws SQLException {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, appNames, environments, users, start, end, names, launchModes, location, rangestatus);
        return requestService.getMainSessionsForSearch(fc);
    }

    @GetMapping("session/main/{appName}/dump")
    public List<Session> getMainSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    ) throws SQLException {
        return requestService.getMainSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/main/{id}")
    public ResponseEntity<Session> getMainSession(@PathVariable String id) throws SQLException { // without tree
        return Optional.ofNullable(requestService.getMainSession(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{id_session}/request/rest")
    public ResponseEntity<List<RestRequest>> getRestRequests(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getRestRequestsLazyForParent(idSession));
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
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getDatabaseRequestsLazy(idSession));
    }

    @GetMapping("session/{id_session}/request/database/{id_database}")
    public ResponseEntity<DatabaseRequest> getDatabaseRequest(@PathVariable(name = "id_session") String idSession,
                                                              @PathVariable(name = "id_database") long idDatabase) throws SQLException {
        return Optional.ofNullable(requestService.getDatabaseRequestComplete(idDatabase))
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
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getFtpRequestsLazy(idSession));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}")
    public ResponseEntity<FtpRequest> getFtpRequest(@PathVariable(name = "id_session") String idSession,
                                                    @PathVariable(name = "id_ftp") long idFtp) throws SQLException {
        return Optional.ofNullable(requestService.getFtpRequestComplete(idFtp))
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
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getSmtpRequestsLazy(idSession));
    }

    @GetMapping("session/{id_session}/request/smtp/{id_smtp}")
    public ResponseEntity<MailRequest> getSmtpRequest(@PathVariable(name = "id_session") String idSession,
                                                      @PathVariable(name = "id_smtp") long idSmtp) throws SQLException {
        return Optional.ofNullable(requestService.getSmtpRequestsComplete(idSmtp))
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
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(requestService.getLdapRequestsLazy(idSession));
    }

    @GetMapping("session/{id_session}/request/ldap/{id_ldap}")
    public ResponseEntity<NamingRequest> getLdapRequest(@PathVariable(name = "id_session") String idSession,
                                                        @PathVariable(name = "id_ldap") long idLdap) throws SQLException {
        return Optional.ofNullable(requestService.getLdapRequestsComplete(idLdap))
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

    @GetMapping("session/{id_session}/user/action")
    public ResponseEntity<List<UserAction>> getUserActions(@PathVariable(name = "id_session") String idSession) throws SQLException {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(analyticService.getUserActions(idSession));
    }

    @GetMapping("session/user/{user}/action")
    public ResponseEntity<List<MainSession>> getUserActions(
            @PathVariable(name = "user") String user,
            @RequestParam(name = "date") Instant date,
            @RequestParam(name = "offset", defaultValue = "0") Integer offSet,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) throws SQLException {
        return ResponseEntity.ok().body(analyticService.getUserActions(user, date, offSet, limit));
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
