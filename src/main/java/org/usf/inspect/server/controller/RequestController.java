package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dto.DtoRequest;
import org.usf.inspect.server.dto.DtoRestRequest;
import org.usf.inspect.server.mapper.InspectMappers;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.inspect.server.service.RequestService;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.Keyword;
import org.usf.jquery.web.QueryRequestFilter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.sql.Timestamp.from;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("instance/{idInstance}")
    public ResponseEntity<InstanceEnvironment> getInstance(
       @QueryRequestFilter(view = "instance",
                           column = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,configuration,resource,additional_properties,id") QueryComposer request,
       @PathVariable String idInstance)  {
        return ok()
                .cacheControl(maxAge(1, DAYS))
                .body(INSPECT.execute(request.filters(INSTANCE.column(ID).varchar().eq(idInstance)), InspectMappers::instanceEnvironmentMapper));
    }

    // New
    @GetMapping("instance/{idInstance}/trace")
    public List<InstanceTrace> getInstanceTraces(
            @QueryRequestFilter(view = "instance_trace",
                    column = "pending,attempts,size_session,filename,start,instance_env") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(INSTANCE_TRACE.column(INSTANCE_ENV).varchar().eq(idInstance)), InspectMappers.instanceTraceMapper());
    }

    @GetMapping("instance/{idInstance}/resource/usage")
    public List<MachineResourceUsage> getInstanceResourceUsages(
            @QueryRequestFilter(view = "resource_usage",
                    column = "low_heap,high_heap,low_meta,high_meta,start") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(RESOURCE_USAGE.column(INSTANCE_ENV).varchar().eq(idInstance)), InspectMappers.instanceResourceUsageMapper());
    }

    @GetMapping("session/{idSession}/log/entry")
    public List<LogEntry> getSessionLogEntries(
            @QueryRequestFilter(view = "log_entry",
                    column = "start,log_level,log_message,parent,instance_env") QueryComposer request,
            @PathVariable String idSession)  {
        return INSPECT.execute(request.filters(LOG_ENTRY.column(PARENT).varchar().eq(idSession)), InspectMappers.instanceLogEntryMapper());
    }

    @GetMapping("request/rest/{idRequest}")
    public ResponseEntity<RestRequestWrapper> getRestRequestById (
            @QueryRequestFilter(view = "rest_request",
                                column = "id,protocol,auth,host,port,path,query,method,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,remote,parent,exception.err_type,exception.err_msg",
                                join = "exception",
                                order = "start") QueryComposer request,
            @PathVariable String idRequest) {
        return  Optional.ofNullable(INSPECT.execute(request.filters(REST_REQUEST.column(ID).varchar().eq(idRequest)), InspectMappers::restRequestMapperComplete))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/{type}/hosts")
    public String[] getRequestsHostsbyType(
            @PathVariable String type,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end)  {
        try {
            RequestType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return requestService.getRequestsHostsByType(type, environment, start, end);
    }

    @GetMapping("request/rest")
    public List<DtoRestRequest> getRestRequests(@RequestParam(required = false, name = "env") String[] environments,
                                                @RequestParam(required = false, name = "host") String[] hosts,
                                                @RequestParam(required = false, name = "start") Instant start,
                                                @RequestParam(required = false, name = "end") Instant end,
                                                @RequestParam(required = false, name = "rangestatus") String [] rangestatus)  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, null, environments, null, start, end, null, null, hosts, null, null, null, null, null, null, null,rangestatus);
        return requestService.getRestRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/database")
    public List<DtoRequest> getDatabaseRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                        @RequestParam(required = false, name = "host") String[] hosts,
                                                        @RequestParam(required = false, name = "start") Instant start,
                                                        @RequestParam(required = false, name = "end") Instant end,
                                                        @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getDatabaseRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/ftp")
    public List<DtoRequest> getFtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                   @RequestParam(required = false, name = "host") String[] hosts,
                                                   @RequestParam(required = false, name = "start") Instant start,
                                                   @RequestParam(required = false, name = "end") Instant end,
                                                   @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getFtpRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/smtp")
    public List<DtoRequest> getSmtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end,rangestatus);
        return requestService.getSmtpRequestsLazyForSearch(jsf);
    }

    @GetMapping("request/ldap")
    public List<DtoRequest> getLdapRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean [] rangestatus)  {
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
            @RequestParam(required = false, name = "rangestatus") String [] rangestatus)  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query,rangestatus);
        return requestService.getRestSessionsForSearch(jsf);
    }

    @GetMapping("session/rest/{appName}/dump")
    public List<Session> getRestSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    )  {
        return requestService.getRestSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/rest/{idSession}")
    public ResponseEntity<Session> getRestSession(
            @QueryRequestFilter(view = "rest_session",
            column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,mask,user,user_agt,cache_control,instance_env") QueryComposer request,
            @PathVariable String idSession) {
        return Optional.ofNullable(INSPECT.execute(request.filters(REST_SESSION.column(ID).varchar().eq(idSession)), InspectMappers::restSessionWithoutInstanceMapper))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/winstance") // todo: find where its used
    public List<Session> getRestSessionWithInstance(
            @QueryRequestFilter(view = "rest_session",
                    column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,mask,user,user_agt,cache_control,instance_env,app_name,os,re,address",
                    join="instance") QueryComposer request,
            @PathVariable String id) {
        return INSPECT.execute(request.filters(REST_SESSION.column(ID).varchar().eq(id)), InspectMappers.restSessionWithInstanceMapper());
    }

    @GetMapping("session/rest/{id}/parent")
    public ResponseEntity<Map<String, String>> getSessionParent(@PathVariable String id)  {
        return Optional.of(requestService.getSessionParent(id))
                .filter(o -> !o.isEmpty())
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{id}/tree")
    public ResponseEntity<Session> getMainTree(@PathVariable String id)  {
        return Optional.ofNullable(requestService.getMainTree(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/tree")
    public ResponseEntity<Session> getRestTree(@PathVariable String id)  {
        return Optional.ofNullable(requestService.getRestTree(id))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main") // can't optimise, done
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
    )  {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(null, appNames, environments, users, start, end, names, launchModes, location, rangestatus);
        return requestService.getMainSessionsForSearch(fc);
    }

    @GetMapping("session/main/{appName}/dump") // can't optimise, done
    public List<Session> getMainSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    )  {
        return requestService.getMainSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/main/{idSession}")
    public ResponseEntity<Session> getMainSession(
            @QueryRequestFilter(
                view = "main_session",
                column = "id,name,start,end,type,location,thread,err_type,err_msg,mask,user,instance_env") QueryComposer request,
            @PathVariable String idSession) {
        return Optional.ofNullable(INSPECT.execute(request.filters(MAIN_SESSION.column(ID).varchar().eq(idSession)), InspectMappers::mainSessionWithoutInstanceMapper))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{idSession}/request/rest")
    public ResponseEntity<List<RestRequestWrapper>>  getRestRequests(
            @QueryRequestFilter(
                view = "rest_request",
                column = "id,protocol,host,path,query,method,status,start,end,thread,remote,parent,exception.err_type,exception.err_msg",
                join = "exception",
                order = "start") QueryComposer request,
            @PathVariable String  idSession){
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(REST_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.restRequestLazyMapper()));
    }

    @GetMapping("session/request/exception") // need to add exception type to front call
    public ResponseEntity<Map<Long, ExceptionInfoWrapper>> getRequestExceptions(
            @QueryRequestFilter(
                    view = "exception",
                    column = "err_type,err_msg,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam( name = "ids") String[] idRequestList)  {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(EXCEPTION.column(PARENT).varchar().in(idRequestList)), InspectMappers::exceptionInfoMapper));
    }

    @GetMapping("session/{idSession}/request/local")
    public ResponseEntity<List<LocalRequestWrapper>> getLocalRequests(
            @QueryRequestFilter(
                    view = "local_request",
                    column = "id,name,location,start,end,user,thread,status,parent,type,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession)  {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(LOCAL_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.localRequestMapper()));
    }

    @GetMapping("session/{idSession}/request/database")
    public ResponseEntity<List<DatabaseRequestWrapper>> getDatabaseRequests(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,db,start,end,user,thread,command,status,schema",
                    order = "start") QueryComposer request, @PathVariable String idSession) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(DATABASE_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.databaseRequestLazyMapper()));
    }

    @GetMapping("session/{idSession}/request/database/{idDatabase}")
    public ResponseEntity<DatabaseRequestWrapper> getDatabaseRequest(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,port,db,start,end,user,thread,driver,db_name,db_version,command,status,schema,parent",
                    order = "start") QueryComposer request, @PathVariable String idSession, @PathVariable String idDatabase) {
        return Optional.ofNullable(INSPECT.execute(request.filters(DATABASE_REQUEST.column(ID).varchar().eq(idDatabase)), InspectMappers::databaseRequestComplete))
                .map(o -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{idSession}/request/database/{idDatabase}/stage")
    public ResponseEntity<List<DatabaseRequestStage>> getDatabaseRequestStages(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "name,start,end,action_count,commands,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession, @PathVariable String idDatabase) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(DATABASE_STAGE.column(PARENT).varchar().eq(idDatabase)), InspectMappers.databaseRequestStageMapper()));
    }

    @GetMapping("session/request/database/stages/count")
    public ResponseEntity<Map<Long, Integer>> getDatabaseRequestStagesCount(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "action_count,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idDatabaseList) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(DATABASE_STAGE.column(PARENT).varchar().in(idDatabaseList)), rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt(ACTION_COUNT.reference()));
            }
            return actionsMap;
        }));
    }

    @GetMapping("session/{idSession}/request/ftp")
    public ResponseEntity<List<FtpRequestWrapper>> getFtpRequests(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,start,end,thread,status",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return  ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(FTP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.ftpRequestLazyMapper()));
    }

    @GetMapping("session/{id_session}/request/ftp/{id_ftp}")
    public ResponseEntity<FtpRequestWrapper> getFtpRequest(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,port,protocol,server_version,client_version,start,end,user,thread,status,parent",
                    order = "start") QueryComposer request,
            @PathVariable(name = "id_session") String idSession,
            @PathVariable(name = "id_ftp") String idFtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(FTP_REQUEST.column(ID).varchar().eq(idFtp)), InspectMappers::ftpRequestComplete))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{idSession}/request/ftp/{idFtp}/stage")
    public ResponseEntity<List<FtpRequestStage>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,start,end,arg,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession, @PathVariable String idFtp) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(INSPECT.execute(request.filters(FTP_STAGE.column(PARENT).varchar().eq(idFtp)), InspectMappers.ftpRequestStageMapper()));
    }

    @GetMapping ("session/request/ftp/stages")
    public ResponseEntity<Map<Long,List<String>>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return  ResponseEntity.ok().body(INSPECT.execute(request.filters(FTP_STAGE.column(PARENT).varchar().in(idFtpList)
                                .and(FTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    @GetMapping("session/{idSession}/request/smtp")
    public ResponseEntity<List<MailRequestWrapper>> getSmtpRequests(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,start,end,thread,status",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(SMTP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.smtpRequestLazyMapper()));
    }

    @GetMapping("session/{idSession}/request/smtp/{idSmtp}")
    public ResponseEntity<MailRequestWrapper> getSmtpRequest(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,port,start,end,user,thread,status,parent",
                    order = "start") QueryComposer request,
            @PathVariable String idSession,
            @PathVariable String idSmtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(SMTP_REQUEST.column(ID).varchar().eq(idSmtp)), InspectMappers::mailRequestCompleteMapper))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{idSession}/request/smtp/{idSmtp}/stage")
    public ResponseEntity<List<MailRequestStage>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,start,end,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession, @PathVariable String idSmtp) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(SMTP_STAGE.column(PARENT).varchar().eq(idSmtp)), InspectMappers.mailRequestStageMapper()));
    }

    @GetMapping("session/{idSession}/request/smtp/{idSmtp}/mail")
    public ResponseEntity<List<MailWrapper>> getSmtpRequestMails(
            @QueryRequestFilter(
                    view = "smtp_mail",
                    column = "subject,from,recipients,media,reply_to,size,parent") QueryComposer request, @PathVariable String idSession, @PathVariable String idSmtp) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(SMTP_MAIL.column(PARENT).varchar().eq(idSmtp)), InspectMappers.mailMapper()));
    }

    @GetMapping("session/request/smtp/stages")
    public ResponseEntity<Map<Long, List<String>>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idSmtpList) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(SMTP_STAGE.column(PARENT).varchar().in(idSmtpList).and(SMTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    @GetMapping("session/request/smtp/stages/count")
    public ResponseEntity<Map<Long, Integer>> getSmtpRequestStagesRowCount(
            @QueryRequestFilter(
                    view = "smtp_mail",
                    column = "parent.count:count,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idSmtpList) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(SMTP_MAIL.column(PARENT).varchar().in(idSmtpList)), rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt("count"));
            }
            return actionsMap;
        }));
    }


    @GetMapping("session/{idSession}/request/ldap")
    public ResponseEntity<List<DirectoryRequestWrapper>> getLdapRequests(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,start,end,thread,status",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(LDAP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.ldapRequestLazyMapper()));
    }

    @GetMapping("session/{idSession}/request/ldap/{idLdap}")
    public ResponseEntity<DirectoryRequestWrapper> getLdapRequests(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,port,protocol,start,end,user,thread,status,parent",
                    order = "start") QueryComposer request,
            @PathVariable String idSession,
            @PathVariable String idLdap){
        return Optional.ofNullable(INSPECT.execute(request.filters(LDAP_REQUEST.column(ID).varchar().eq(idLdap)), InspectMappers::ldapRequestCompleteMapper))
                .map(o -> ok().cacheControl(maxAge(1, DAYS)).body(o))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }


    @GetMapping("session/{idSession}/request/ldap/{idLdap}/stage")
    public ResponseEntity<List<DirectoryRequestStage>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,start,end,arg,parent,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession, @PathVariable String idLdap) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(LDAP_STAGE.column(PARENT).varchar().eq(idLdap)), InspectMappers.ldapRequestStageMapper()));
    }

    @GetMapping ("session/request/ldap/stages")
    public ResponseEntity<Map<Long,List<String>>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(LDAP_STAGE.column(PARENT).varchar().in(idFtpList).and(LDAP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    @GetMapping ("session/{idSession}/user/action")
    public ResponseEntity<List<UserAction>> getUserActions(
            @QueryRequestFilter(
                    view = "user_action",
                    column = "name,node_name,type,start,parent",
                    order = "start") QueryComposer request,
            @PathVariable String idSession) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(USER_ACTION.column(PARENT).varchar().eq(idSession)), InspectMappers.userActionMapper()));
    }

    @GetMapping ("session/user/{user}/action")
    public ResponseEntity<List<MainSessionWrapper>> getUserActions(
            @QueryRequestFilter(
                    view = "main_session",
                    column = "id,start:session_start,end,location,name:session_name,user_action.name:action_name,user_action.node_name,user_action.type,user_action.start:action_start",
                    join = "user_action",
                    order = "main_session.start,user_action.start",
                    ignoreParameters = "date",
                    mergeParameters = {Keyword.LIMIT,Keyword.OFFSET}) QueryComposer request,
            @PathVariable(name = "user") String user,
            @RequestParam(name = "date") Instant date
            ) {
        return ResponseEntity.ok().body(INSPECT.execute(request.filters(MAIN_SESSION.column(USER).eq(user).and(MAIN_SESSION.column(START).ge(from(date)))), rs -> {
            List<MainSessionWrapper> sessions = new ArrayList<>();
            while (rs.next()) {
                var userAction =  new UserAction(
                        rs.getString("action_name"),
                        rs.getString(NODE_NAME.reference()),
                        rs.getString(TYPE.reference()),
                        fromNullableTimestamp(rs.getTimestamp("action_start"))
                );
                var cdSession = rs.getString(ID.reference());
                var session = sessions.stream().filter(s -> s.getId().equals(cdSession)).findFirst().orElse(null);
                if(session == null) {
                    session = new MainSessionWrapper();
                    session.setId(rs.getString(ID.reference()));
                    session.setStart(fromNullableTimestamp(rs.getTimestamp("session_start")));
                    session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                    session.setName(rs.getString("session_name"));
                    session.setLocation(rs.getString(LOCATION.reference()));
                    if(userAction.getStart() == null) {
                        session.setUserActions(new ArrayList<>());
                    } else {
                        session.setUserActions(new ArrayList<>(List.of(userAction)));
                    }

                    sessions.add(session);
                } else {
                    session.getUserActions().add(userAction);
                }
            }
            return sessions;
        }));
    }

    @GetMapping("architecture")
    public ResponseEntity<List<Architecture>> getArchitecture(
            @RequestParam(required = false, name = "start") Instant start,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestParam(required = false, name = "env") String[] environments
    )  {
        return ResponseEntity.ok().body(requestService.createArchitecture(start, end, environments));
    }
}
