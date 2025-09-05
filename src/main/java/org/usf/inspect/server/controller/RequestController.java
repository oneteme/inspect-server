package org.usf.inspect.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dto.*;
import org.usf.inspect.server.mapper.InspectMappers;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.service.RequestService;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.Keyword;
import org.usf.jquery.web.QueryRequestFilter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.sql.Timestamp.from;
import static java.util.Objects.nonNull;
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
    private final ObjectMapper mapper;

    @GetMapping("instance/{idInstance}")
    public ResponseEntity<InstanceEnvironment> getInstance(
       @QueryRequestFilter(view = "instance",
                           column = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,resource,configuration,id") QueryComposer request,
       @PathVariable String idInstance)  {
        return ok()
                .cacheControl(maxAge(1, DAYS))
                .body(INSPECT.execute(request.filters(INSTANCE.column(ID).varchar().eq(idInstance)), InspectMappers.instanceEnvironmentMapper(mapper)));
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

    @GetMapping("instance/{idInstance}/log/entry")
    public List<LogEntry> getLogEntries(
            @QueryRequestFilter(view = "log_entry",
                    column = "start,log_level,log_message,stacktrace", order = "start.desc") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(LOG_ENTRY.column(INSTANCE_ENV).varchar().eq(idInstance)), InspectMappers.instanceLogEntryMapper(mapper));
    }

    @GetMapping("request/{type}/hosts")
    public String[] getRequestHosts(
            @PathVariable String type,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end)  {
        TraceApiTable requestTable;
        try {
            requestTable = RequestType.valueOf(type).getTable();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return requestService.getRequestHosts(requestTable, environment, start, end);
    }

    @GetMapping("request/rest")
    public List<RestRequestDto> getRestRequests(@RequestParam(required = false, name = "env") String[] environments,
                                                @RequestParam(required = false, name = "host") String[] hosts,
                                                @RequestParam(required = false, name = "start") Instant start,
                                                @RequestParam(required = false, name = "end") Instant end,
                                                @RequestParam(required = false, name = "rangestatus") String[] rangestatus)  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, null, environments, null, start, end, null, null, hosts, null, null, null, null, null, null, null, rangestatus);
        return requestService.getRestRequests(jsf);
    }

    @GetMapping("request/database")
    public List<DatabaseRequestDto> getDatabaseRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                        @RequestParam(required = false, name = "host") String[] hosts,
                                                        @RequestParam(required = false, name = "start") Instant start,
                                                        @RequestParam(required = false, name = "end") Instant end,
                                                        @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments, hosts, start, end, rangestatus);
        return requestService.getDatabaseRequests(jsf);
    }

    @GetMapping("request/ftp")
    public List<FtpRequestDto> getFtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                   @RequestParam(required = false, name = "host") String[] hosts,
                                                   @RequestParam(required = false, name = "start") Instant start,
                                                   @RequestParam(required = false, name = "end") Instant end,
                                                   @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end, rangestatus);
        return requestService.getFtpRequests(jsf);
    }

    @GetMapping("request/smtp")
    public List<MailRequestDto> getSmtpRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end, rangestatus);
        return requestService.getSmtpRequestsByFilter(jsf);
    }

    @GetMapping("request/ldap")
    public List<DirectoryRequestDto> getLdapRequestForSearch(@RequestParam(required = false, name = "env") String[] environments,
                                                    @RequestParam(required = false, name = "host") String[] hosts,
                                                    @RequestParam(required = false, name = "start") Instant start,
                                                    @RequestParam(required = false, name = "end") Instant end,
                                                    @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus)  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end, rangestatus);
        return requestService.getLdapRequestsByFilter(jsf);
    }


    @GetMapping("session/rest")
    public List<RestSessionDto> getRestSessions(
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
            @RequestParam(required = false, name = "rangestatus") String[] rangestatus)  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query,rangestatus);
        return requestService.getRestSessionsForSearch(jsf);
    }

    @GetMapping("session/rest/{appName}/dump")
    public List<RestSession> getRestSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    )  {
        return requestService.getRestSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/rest/{idSession}")
    public ResponseEntity<RestSession> getRestSession(
            @QueryRequestFilter(view = "rest_session",
            column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,mask,user,user_agt,cache_control,instance_env") QueryComposer request,
            @PathVariable String idSession) {
        return Optional.ofNullable(INSPECT.execute(request.filters(REST_SESSION.column(ID).varchar().eq(idSession)), InspectMappers::createBaseRestSession))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("{type}/{id}/parent")
    public ResponseEntity<Map<String, String>> getSessionParent(
            @PathVariable String type,
            @PathVariable String id
    )  {
        TraceApiTable tableType;
        try {
            tableType = RequestType.valueOf(type).getTable();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return Optional.of(requestService.getSessionParent(tableType, id))
                .filter(o -> !o.isEmpty())
                .map(o -> ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{id}/tree")
    public ResponseEntity<Session> getMainTree(@PathVariable String id)  {
        return Optional.ofNullable(requestService.getMainTree(id))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{id}/tree")
    public ResponseEntity<Session> getRestTree(@PathVariable String id)  {
        return Optional.ofNullable(requestService.getRestTree(id))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main") // can't optimise, done
    public List<MainSessionDto> getMainSessions(
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
    public List<MainSession> getMainSessionsForDump(
            @PathVariable String appName,
            @RequestParam(name = "env") String env,
            @RequestParam(name = "start") Instant start,
            @RequestParam(name = "end") Instant end
    )  {
        return requestService.getMainSessionsForDump(env, appName, start, end);
    }

    @GetMapping("session/main/{idSession}")
    public ResponseEntity<MainSession> getMainSession(
            @QueryRequestFilter(
                view = "main_session",
                column = "id,name,start,end,type,location,thread,err_type,err_msg,mask,user,instance_env") QueryComposer request,
            @PathVariable String idSession) {
        return Optional.ofNullable(INSPECT.execute(request.filters(MAIN_SESSION.column(ID).varchar().eq(idSession)), InspectMappers::createBaseMainsession))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/{idSession}/request/rest")
    public ResponseEntity<List<RestRequestDto>>  getRestRequests(
            @QueryRequestFilter(
                view = "rest_request",
                column = "id,protocol,host,path,query,method,status,start,end,thread,body_content,exception.err_type,exception.err_msg",
                join = "exception",
                order = "start") QueryComposer request,
            @PathVariable String  idSession){
        return ok().body(INSPECT.execute(request.filters(REST_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.restRequestLazyMapper()));
    }

    @GetMapping("session/request/exception") // need to add exception type to front call
    public ResponseEntity<Map<Long, ExceptionInfo>> getRequestExceptions(
            @QueryRequestFilter(
                    view = "exception",
                    column = "err_type,err_msg,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam( name = "ids") String[] idRequestList)  {
        return ok().body(INSPECT.execute(request.filters(EXCEPTION.column(PARENT).varchar().in(idRequestList)), InspectMappers::exceptionInfoMapper));
    }

    @GetMapping("session/{idSession}/request/local")
    public ResponseEntity<List<LocalRequest>> getLocalRequests(
            @QueryRequestFilter(
                    view = "local_request",
                    column = "id,name,location,start,end,user,thread,type,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession)  {
        return ok().body(INSPECT.execute(request.filters(LOCAL_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.localRequestMapper()));
    }

    @GetMapping("session/{idSession}/request/database")
    public ResponseEntity<List<DatabaseRequestDto>> getDatabaseRequests(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,db,start,end,user,thread,command,schema,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession) {
        return ok().body(INSPECT.execute(request.filters(DATABASE_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.databaseRequestLazyMapper()));
    }


    @GetMapping("request/rest/{idRequest}")
    public ResponseEntity<RestRequest> getRestRequest (
            @QueryRequestFilter(view = "rest_request",
                    column = "id,protocol,auth,host,port,path,query,method,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,body_content,parent") QueryComposer request,
            @PathVariable String idRequest) {
        return  Optional.ofNullable(INSPECT.execute(request.filters(REST_REQUEST.column(ID).varchar().eq(idRequest)), InspectMappers::restRequestMapperComplete))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/rest/{idRequest}/stage")
    public ResponseEntity<List<HttpRequestStage>> getRestRequestStages (
            @QueryRequestFilter(view = "rest_request_stage",
                    column = "name,order,start,end,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "order") QueryComposer request,
            @PathVariable String idRequest) {
        return ok().body(INSPECT.execute(request.filters(REST_REQUEST_STAGE.column(PARENT).varchar().eq(idRequest)), InspectMappers.restRequestStageMapper()));
    }

    @GetMapping("request/database/{idDatabase}")
    public ResponseEntity<DatabaseRequest> getDatabaseRequest(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,port,db,start,end,user,thread,driver,db_name,db_version,command,schema,parent") QueryComposer request, @PathVariable String idDatabase) {
        return Optional.ofNullable(INSPECT.execute(request.filters(DATABASE_REQUEST.column(ID).varchar().eq(idDatabase)), InspectMappers::databaseRequestComplete))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/database/{idDatabase}/stage")
    public ResponseEntity<List<DatabaseRequestStage>> getDatabaseRequestStages(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "name,order,start,end,action_count,command,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idDatabase) {
        return ok().body(INSPECT.execute(request.filters(DATABASE_STAGE.column(PARENT).varchar().eq(idDatabase)), InspectMappers.databaseRequestStageMapper()));
    }

    @GetMapping("session/request/database/stages/count")
    public ResponseEntity<Map<Long, Integer>> getDatabaseRequestStagesCount(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "action_count,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idDatabaseList) {
        return ok().body(INSPECT.execute(request.filters(DATABASE_STAGE.column(PARENT).varchar().in(idDatabaseList)), rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt(ACTION_COUNT.reference()));
            }
            return actionsMap;
        }));
    }

    @GetMapping("session/{idSession}/request/ftp")
    public ResponseEntity<List<FtpRequestDto>> getFtpRequests(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,start,end,thread,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return  ok().body(INSPECT.execute(request.filters(FTP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.ftpRequestLazyMapper()));
    }

    @GetMapping("request/ftp/{idFtp}")
    public ResponseEntity<FtpRequest> getFtpRequest(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,port,protocol,server_version,client_version,start,end,user,thread,parent") QueryComposer request,
            @PathVariable String idFtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(FTP_REQUEST.column(ID).varchar().eq(idFtp)), InspectMappers::ftpRequestComplete))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/ftp/{idFtp}/stage")
    public ResponseEntity<List<FtpRequestStage>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,order,start,end,arg,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idFtp) {
        return ok().body(INSPECT.execute(request.filters(FTP_STAGE.column(PARENT).varchar().eq(idFtp)), InspectMappers.ftpRequestStageMapper()));
    }

    @GetMapping ("session/request/ftp/stages")
    public ResponseEntity<Map<Long,List<String>>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return  ok().body(INSPECT.execute(request.filters(FTP_STAGE.column(PARENT).varchar().in(idFtpList)
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
    public ResponseEntity<List<MailRequestDto>> getSmtpRequests(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,start,end,thread,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ok().body(INSPECT.execute(request.filters(SMTP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.smtpRequestLazyMapper()));
    }

    @GetMapping("request/smtp/{idSmtp}")
    public ResponseEntity<MailRequest> getSmtpRequest(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,port,start,end,user,thread,parent") QueryComposer request,
            @PathVariable String idSmtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(SMTP_REQUEST.column(ID).varchar().eq(idSmtp)), InspectMappers::mailRequestCompleteMapper))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/smtp/{idSmtp}/stage")
    public ResponseEntity<List<MailRequestStage>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,order,start,end,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idSmtp) {
        return ok().body(INSPECT.execute(request.filters(SMTP_STAGE.column(PARENT).varchar().eq(idSmtp)), InspectMappers.mailRequestStageMapper()));
    }

    @GetMapping("request/smtp/{idSmtp}/mail")
    public ResponseEntity<List<Mail>> getSmtpRequestMails(
            @QueryRequestFilter(
                    view = "smtp_mail",
                    column = "subject,from,recipients,media,reply_to,size") QueryComposer request, @PathVariable String idSmtp) {
        return ok().body(INSPECT.execute(request.filters(SMTP_MAIL.column(PARENT).varchar().eq(idSmtp)), InspectMappers.mailMapper()));
    }

    @GetMapping("session/request/smtp/stages")
    public ResponseEntity<Map<Long, List<String>>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idSmtpList) {
        return ok().body(INSPECT.execute(request.filters(SMTP_STAGE.column(PARENT).varchar().in(idSmtpList).and(SMTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
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
        return ok().body(INSPECT.execute(request.filters(SMTP_MAIL.column(PARENT).varchar().in(idSmtpList)), rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt("count"));
            }
            return actionsMap;
        }));
    }


    @GetMapping("session/{idSession}/request/ldap")
    public ResponseEntity<List<DirectoryRequestDto>> getLdapRequests(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,start,end,thread,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ok().body(INSPECT.execute(request.filters(LDAP_REQUEST.column(PARENT).varchar().eq(idSession)), InspectMappers.ldapRequestLazyMapper()));
    }

    @GetMapping("request/ldap/{idLdap}")
    public ResponseEntity<DirectoryRequest> getLdapRequest(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,port,protocol,start,end,user,thread,parent") QueryComposer request,
            @PathVariable String idLdap){
        return Optional.ofNullable(INSPECT.execute(request.filters(LDAP_REQUEST.column(ID).varchar().eq(idLdap)), InspectMappers::ldapRequestCompleteMapper))
                .map(o -> nonNull(o.getEnd()) ? ok().cacheControl(maxAge(1, DAYS)).body(o) : ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }


    @GetMapping("request/ldap/{idLdap}/stage")
    public ResponseEntity<List<DirectoryRequestStage>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,order,start,end,arg,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idLdap) {
        return ok().body(INSPECT.execute(request.filters(LDAP_STAGE.column(PARENT).varchar().eq(idLdap)), InspectMappers.ldapRequestStageMapper()));
    }

    @GetMapping ("session/request/ldap/stages")
    public ResponseEntity<Map<Long,List<String>>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return ok().body(INSPECT.execute(request.filters(LDAP_STAGE.column(PARENT).varchar().in(idFtpList).and(LDAP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
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
        return ok().body(INSPECT.execute(request.filters(USER_ACTION.column(PARENT).varchar().eq(idSession)), InspectMappers.userActionMapper()));
    }

    @GetMapping ("session/user/{user}/action")
    public ResponseEntity<List<AnalyticDto>> getUserActions(
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
        return ok().body(INSPECT.execute(request.filters(MAIN_SESSION.column(USER).eq(user).and(MAIN_SESSION.column(START).ge(from(date)))), rs -> {
            List<AnalyticDto> sessions = new ArrayList<>();
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
                    session = new AnalyticDto();
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
        return ok().body(requestService.createArchitecture(start, end, environments));
    }
}
