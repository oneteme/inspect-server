package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dto.*;
import org.usf.inspect.server.erm.InspectStore;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.validation.Condition;
import org.usf.inspect.server.validation.Validate;
import org.usf.jquery.mvc.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.UUID.fromString;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.erm.ViewRegistryConstant.*;
import static org.usf.jquery.mvc.QueryExtension.Modifier.REJECT;

@Slf4j
@CrossOrigin
@Validated
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("{type}/{id}/parent")
    public ResponseEntity<Map<String, String>> getSessionParent(
            @PathVariable String type, //TODO change string to RequestType
            @PathVariable String id
    )  {
        RequestType requestType;
        try {
            requestType = RequestType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return Optional.of(requestService.getSessionParent(requestType, id))
                .filter(o -> !o.isEmpty())
                .map(o -> ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/{type}/hosts")
    public Collection<String> getRequestHosts(
            @PathVariable String type,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(name = "end") @Validate(Condition.INSTANT) Instant end)  {
        RequestType requestTable;
        try {
            requestTable = RequestType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return requestService.getRequestHosts(requestTable, environment, start, end);
    }

    @GetMapping("instance/{instanceId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(
            dataset = "instance",
            view = INSTANCE_ENVIRONMENT_RESULTSET_MAPPER,
            select = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,resource,configuration,id")
    public ResponseEntity<InstanceEnvironment> fetchInstance(
            MvcRequest mvc,
            @PathVariable String instanceId
    ) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().id().eq(fromString(instanceId))); // UUID

        return ok().cacheControl(maxAge(1, HOURS)).body((InstanceEnvironment) mvc.execute());
    }

    @GetMapping("instance/{instanceId}/trace")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(
            dataset = "instance_trace",
            view = INSTANCE_TRACE_ROW_MAPPER,
            select = "pending,attempts,size_session,filename,start,instance_env")
    public Collection<TracePacket> fetchInstanceTraces(
            MvcRequest mvc,
            @PathVariable String instanceId
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instanceTrace().instanceEnv().eq(fromString(instanceId))); // UUID

        return (Collection<TracePacket>) mvc.execute();
    }

    @GetMapping("instance/{instanceId}/resource/usage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(
            dataset = "resource_usage",
            view = MACHINE_RESOURCE_USAGE_ROW_MAPPER,
            select = "low_heap,high_heap,start")
    public Collection<MachineResourceUsage> fetchResourceUsages(
            MvcRequest mvc,
            @PathVariable String instanceId
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.resourceUsage().instanceEnv().eq(fromString(instanceId))); // UUID

        return (Collection<MachineResourceUsage>) mvc.execute();
    }

    @GetMapping("instance/{instanceId}/log/entry")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "log_entry",
            view = LOG_ENTRY_ROW_MAPPER,
            select = "start,log_level,log_message,stacktrace",
            order = "start.desc")
    public Collection<LogEntry> fetchLogEntriesByInstance(
            MvcRequest mvc,
            @PathVariable String instanceId
    ) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.logEntry().instanceEnv().eq(fromString(instanceId))); // UUID

        return (Collection<LogEntry>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/log/entry")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "log_entry",
            view = LOG_ENTRY_ROW_MAPPER,
            select = "start,log_level,log_message,stacktrace",
            order = "start.desc")
    public Collection<LogEntry> fetchLogEntriesBySession(
            MvcRequest mvc,
            @PathVariable String sessionId
    ) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.logEntry().parent().eq(fromString(sessionId))); // UUID

        return (Collection<LogEntry>) mvc.execute();
    }

    @GetMapping("session/request/exception") // need to add exception type to front call
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "exception",
            view = EXCEPTION_BY_REQUEST_RESULTSET_MAPPER,
            select = "err_type,err_msg,parent",
            ignore = "requestIds")
    public Map<Long, ExceptionTrace> fetchExceptionByRequests(
            MvcRequest mvc,
            @RequestParam( name = "requestIds") String[] requestIds)  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.exception().parent().in(Arrays.stream(requestIds).map(UUID::fromString).toArray())); // UUID

        return (Map<Long, ExceptionTrace>) mvc.execute();
    }

    @GetMapping("session/request/database/stages/count")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "database_stage",
            select = "action_count,parent",
            ignore = "requestIds")
    public Map<String, Integer> fetchDatabaseStageCountByRequests(
            MvcRequest mvc,
            @RequestParam(name = "requestIds") String[] requestIds) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.databaseRequestStage().parent().in(Arrays.stream(requestIds).map(UUID::fromString).toArray()));

        return store.execute(mvc.getComposer().compose(store), rs -> {
            Map<String, Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getString("parent"), rs.getInt("actionCount"));
            }
            return actionsMap;
        });
    }

//    @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
//    @RequestParam(required = false, name = "name") String[] names,
//    @RequestParam(required = false, name = "launchmode") String[] launchModes,
//    @RequestParam(required = false, name = "location") String location,
//    @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
//    @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
//    @RequestParam(required = false, name = "user") String[] users,
//    @RequestParam(required = false, name = "appname") String[] appNames,
//    @RequestParam(required = false, name = "failed") Boolean[] failed,
//    @RequestParam(required = false, name = "lazy") boolean lazy

    @GetMapping("session/main") // can't optimise, done
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "main_session",
            view = MAIN_SESSION_ROW_MAPPER,
            select = "id,type,name,start,end,user,location,status,instance.address,instance.app_name",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<MainSessionDto> fetchMainSessions(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<MainSessionDto>) mvc.execute();
    }

    @GetMapping("session/main/{sessionId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "main_session",
            view = MAIN_SESSION_RESULTSET_MAPPER,
            select = "id,name,start,end,type,location,thread,err_type,err_msg,stacktrace,mask,user,instance_env")
    public ResponseEntity<MainSession> fetchMainSession(
            MvcRequest mvc,
            @PathVariable String sessionId
    ) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.mainSession().id().eq(fromString(sessionId))); // UUID

        return Optional.ofNullable((MainSession) mvc.execute())
                .map(o -> ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/main/{sessionId}/tree")
    public ResponseEntity<Session> getMainTree(@PathVariable UUID sessionId)  {
        try {
            var result = requestService.getMainTree(sessionId);
            return result.wasCompleted() ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result) : ok().body(result);
        } catch (NoSuchElementException e) {
            return status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("instance/{instanceId}/session/main")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "main_session",
            view = MAIN_SESSION_PULSE_ROW_MAPPER,
            select = "id,name,start,end,thread,type,location",
            order = "start,end")
    public Collection<MainSession> fetchMainSessionsByInstance(
            MvcRequest mvc,
            @PathVariable String instanceId
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.mainSession().instanceEnv().eq(fromString(instanceId))); // UUID

        return (Collection<MainSession>) mvc.execute();
    }

//    @RequestParam(required = false, name = "method") String[] methods,
//    @RequestParam(required = false, name = "protocol") String[] protocols,
//    @RequestParam(required = false, name = "host") String[] hosts,
//    @RequestParam(required = false, name = "port") String[] ports,
//    @RequestParam(required = false, name = "path") String path,
//    @RequestParam(required = false, name = "query") String query,
//    @RequestParam(required = false, name = "media") String[] medias,
//    @RequestParam(required = false, name = "auth") String[] auths,
//    @RequestParam(required = false, name = "status") Integer[] status,
//    @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
//    @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
//    @RequestParam(required = false, name = "apiname") String[] apiNames,
//    @RequestParam(required = false, name = "user") String[] users,
//    @RequestParam(required = false, name = "appname") String[] appNames,
//    @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
//    @RequestParam(required = false, name = "rangestatus") String[] rangestatus,
//    @RequestParam(required = false, name = "lazy") boolean lazy

    @GetMapping("session/rest")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_session",
            view = REST_SESSION_ROW_MAPPER,
            select = "id,api_name,method,protocol,path,query,status,start,end,user,instance.app_name",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<RestSessionDto> fetchRestSessions(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<RestSessionDto>) mvc.execute();
    }

    @GetMapping("session/rest/{sessionId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_session",
            view = REST_SESSION_RESULTSET_MAPPER,
            select = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,stacktrace,mask,user,user_agt,cache_control,linked,instance_env")
    public ResponseEntity<RestSession> fetchRestSession(
            MvcRequest mvc,
            @PathVariable String sessionId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restSession().id().eq(fromString(sessionId))); // UUID

        return Optional.ofNullable((RestSession) mvc.execute())
                .map(o -> ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("session/rest/{sessionId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_session_stage",
            view = REST_SESSION_STAGE_ROW_MAPPER,
            select = "name,order,start,end",
            order = "order")
    public Collection<HttpSessionStage> fetchRestSessionStages (
            MvcRequest mvc,
            @PathVariable String sessionId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restSessionStage().parent().eq(fromString(sessionId))); // UUID

        return (Collection<HttpSessionStage>) mvc.execute();
    }

    @GetMapping("session/rest/{sessionId}/tree")
    public ResponseEntity<Session> getRestTree(@PathVariable UUID sessionId)  {
        try {
            var result = requestService.getRestTree(sessionId);
            return result.wasCompleted() ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result) : ok().body(result);
        } catch (NoSuchElementException e) {
            return status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("instance/{instanceId}/session/rest")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_session",
            view = REST_SESSION_PULSE_ROW_MAPPER,
            select = "id,api_name,method,host,path,status,start,end,thread,user",
            order = "start,end")
    public Collection<RestSession> fetchRestSessionsForPulse(
            MvcRequest mvc,
            @PathVariable String instanceId
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restSession().instanceEnv().eq(fromString(instanceId))); // UUID

        return (Collection<RestSession>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/rest")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_request",
            view = REST_REQUEST_ROW_MAPPER,
            select = "id,protocol,host,path,query,method,status,start,end,thread,user,body_content,linked,parent",
            order = "start")
    public Collection<RestRequestDto> fetchRestRequestsBySession(
            MvcRequest mvc,
            @PathVariable String sessionId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<RestRequestDto>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/local")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "local_request",
            view = LOCAL_REQUEST_ROW_MAPPER,
            select = "id,name,location,start,end,user,thread,type,exception.err_type,exception.err_msg",
            join = "exception",
            order = "start")
    public Collection<LocalRequest> fetchLocalRequests(
            MvcRequest mvc,
            @PathVariable String sessionId)  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.localRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<LocalRequest>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/database")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "database_request",
            view = DATABASE_REQUEST_ROW_MAPPER,
            select = "id,host,db,db_name,start,end,user,thread,command,schema,parent",
            order = "start")
    public Collection<DatabaseRequestDto> fetchDatabaseRequestsBySession(
            MvcRequest mvc,
            @PathVariable String sessionId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.databaseRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<DatabaseRequestDto>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/ftp")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ftp_request",
            view = FTP_REQUEST_ROW_MAPPER,
            select = "id,host,start,end,thread,user,command,parent",
            order = "start")
    public Collection<FtpRequestDto> fetchFtpRequestsBySession(
            MvcRequest mvc,
            @PathVariable String sessionId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ftpRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<FtpRequestDto>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/smtp")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "smtp_request",
            view = SMTP_REQUEST_ROW_MAPPER,
            select = "id,host,start,end,thread,user,command,parent",
            order = "start")
    public Collection<MailRequestDto> fetchSmtpRequestsBySession(
            MvcRequest mvc,
            @PathVariable String sessionId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.smtpRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<MailRequestDto>) mvc.execute();
    }

    @GetMapping("session/{sessionId}/request/ldap")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ldap_request",
            view = LDAP_REQUEST_ROW_MAPPER,
            select = "id,host,start,end,thread,user,command,parent",
            order = "start")
    public Collection<DirectoryRequestDto> fetchLdapRequestsBySession(
            MvcRequest mvc,
            @PathVariable String sessionId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ldapRequest().parent().eq(fromString(sessionId))); // UUID

        return (Collection<DirectoryRequestDto>) mvc.execute();
    }

//    @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
//    @RequestParam(required = false, name = "host") String[] hosts,
//    @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
//    @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
//    @RequestParam(required = false, name = "rangestatus") String[] rangestatus,
//    @RequestParam(required = false, name = "lazy") boolean lazy

    @GetMapping("request/rest")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_request",
            view = REST_REQUEST_ROW_MAPPER,
            select = "id,protocol,host,path,query,method,status,start,end,thread,user,body_content,linked,parent",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<RestRequestDto> fetchRestRequests(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<RestRequestDto>) mvc.execute();
    }

    @GetMapping("request/rest/{requestId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_request",
            view = REST_REQUEST_RESULTSET_MAPPER,
            select = "id,protocol,auth,host,port,path,query,method,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,user,body_content,linked,instance_env,parent")
    public ResponseEntity<RestRequest> fetchRestRequest(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restRequest().id().eq(fromString(requestId))); // UUID

        return Optional.ofNullable((RestRequest) mvc.execute())
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/rest/{requestId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "rest_request_stage",
            view = REST_REQUEST_STAGE_ROW_MAPPER,
            select = "name,order,start,end,exception.err_type,exception.err_msg,exception.stacktrace",
            join = "exception",
            order = "order")
    public Collection<HttpRequestStage> fetchRestRequestStages (
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.restRequestStage().parent().eq(fromString(requestId))); // UUID

        return (Collection<HttpRequestStage>) mvc.execute();
    }

    @GetMapping("request/database")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "database_request",
            view = DATABASE_REQUEST_ROW_MAPPER,
            select = "id,host,db,db_name,status,start,end,user,thread,command,schema,parent",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<DatabaseRequestDto> fetchDatabaseRequests(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<DatabaseRequestDto>) mvc.execute();
    }

    @GetMapping("request/database/{requestId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "database_request",
            view = DATABASE_REQUEST_RESULTSET_MAPPER,
            select = "id,host,port,db,start,end,user,thread,driver,db_name,db_version,command,schema,instance_env,parent")
    public ResponseEntity<DatabaseRequest> fetchDatabaseRequest(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.databaseRequest().id().eq(fromString(requestId))); // UUID

        return Optional.ofNullable((DatabaseRequest) mvc.execute())
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/database/{requestId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "database_stage",
            view = DATABASE_REQUEST_STAGE_ROW_MAPPER,
            select = "name,order,start,end,arg,action_count,command,exception.err_type,exception.err_msg,exception.stacktrace",
            join = "exception",
            order = "order")
    public Collection<DatabaseRequestStage> fetchDatabaseRequestStages(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.databaseRequestStage().parent().eq(fromString(requestId))); // UUID

        return (Collection<DatabaseRequestStage>) mvc.execute();
    }

    @GetMapping("request/ftp")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ftp_request",
            view = FTP_REQUEST_ROW_MAPPER,
            select = "id,host,status,start,end,thread,user,command,parent",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<FtpRequestDto> fetchFtpRequests(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<FtpRequestDto>) mvc.execute();
    }

    @GetMapping("request/ftp/{requestId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ftp_request",
            view = FTP_REQUEST_RESULTSET_MAPPER,
            select = "id,host,port,protocol,server_version,client_version,start,end,user,thread,command,instance_env,parent")
    public ResponseEntity<FtpRequest> fetchFtpRequest(
            MvcRequest mvc,
            @PathVariable String requestId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ftpRequest().id().eq(fromString(requestId))); // UUID

        return Optional.ofNullable((FtpRequest) mvc.execute())
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/ftp/{requestId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ftp_stage",
            view = FTP_REQUEST_STAGE_ROW_MAPPER,
            select = "name,order,start,end,command,arg,exception.err_type,exception.err_msg,exception.stacktrace",
            join = "exception",
            order = "order")
    public Collection<FtpRequestStage> fetchFtpRequestStages(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ftpStage().parent().eq(fromString(requestId))); // UUID

        return (Collection<FtpRequestStage>) mvc.execute();
    }

    @GetMapping("request/smtp")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "smtp_request",
            view = SMTP_REQUEST_ROW_MAPPER,
            select = "id,host,status,start,end,thread,user,command,parent",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<MailRequestDto> fetchSmtpRequests(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<MailRequestDto>) mvc.execute();
    }

    @GetMapping("request/smtp/{requestId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "smtp_request",
            view = SMTP_REQUEST_RESULTSET_MAPPER,
            select = "id,host,port,start,end,user,thread,command,instance_env,parent")
    public ResponseEntity<MailRequest> fetchSmtpRequest(
            MvcRequest mvc,
            @PathVariable String requestId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.smtpRequest().id().eq(fromString(requestId))); // UUID

        return Optional.ofNullable((MailRequest) mvc.execute())
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("request/smtp/{requestId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "smtp_stage",
            view = SMTP_REQUEST_STAGE_ROW_MAPPER,
            select = "name,order,start,command,end,exception.err_type,exception.err_msg,exception.stacktrace",
            join = "exception",
            order = "order")
    public Collection<MailRequestStage> fetchSmtpRequestStages(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.smtpStage().parent().eq(fromString(requestId))); // UUID

        return (Collection<MailRequestStage>) mvc.execute();
    }

    @GetMapping("request/smtp/{requestId}/mail")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "smtp_mail",
            view = SMTP_REQUEST_MAIL_ROW_MAPPER,
            select = "subject,from,recipients,media,reply_to,size")
    public Collection<Mail> fetchSmtpRequestMails(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.smtpMail().parent().eq(fromString(requestId))); // UUID

        return (Collection<Mail>) mvc.execute();
    }

    @GetMapping("request/ldap")
    @QueryGuard(maxRows = 300000)
    @QueryExtension(select = REJECT, join = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ldap_request",
            view = LDAP_REQUEST_ROW_MAPPER,
            select = "id,host,status,start,end,thread,user,command,parent",
            join = "instance",
            order = "start",
            ignore = "env")
    public Collection<DirectoryRequestDto> fetchLdapRequests(
            MvcRequest mvc,
            @RequestParam(name = "env") String environment
    )  {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.instance().environement().eq(environment));

        return (Collection<DirectoryRequestDto>) mvc.execute();
    }

    @GetMapping("request/ldap/{requestId}")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ldap_request",
            view = LDAP_REQUEST_RESULTSET_MAPPER,
            select = "id,host,port,protocol,start,end,user,command,thread,instance_env,parent")
    public ResponseEntity<DirectoryRequest> fetchLdapRequest(
            MvcRequest mvc,
            @PathVariable String requestId){
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ldapRequest().id().eq(fromString(requestId))); // UUID

        return Optional.ofNullable((DirectoryRequest) mvc.execute())
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }


    @GetMapping("request/ldap/{requestId}/stage")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "ldap_stage",
            view = LDAP_REQUEST_STAGE_ROW_MAPPER,
            select = "name,order,start,end,command,arg,exception.err_type,exception.err_msg,exception.stacktrace",
            join = "exception",
            order = "order")
    public Collection<DirectoryRequestStage> fetchLdapRequestStages(
            MvcRequest mvc,
            @PathVariable String requestId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.ldapStage().parent().eq(fromString(requestId))); // UUID

        return (Collection<DirectoryRequestStage>) mvc.execute();
    }

    @GetMapping("architecture")
    public ResponseEntity<Collection<Architecture>> getArchitecture(
            @RequestParam(name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(name = "end") @Validate(Condition.INSTANT) Instant end,
            @RequestParam(name = "env") @Validate(Condition.NOT_EMPTY) String[] environments
    )  {
        var result = requestService.createArchitecture(start, end, environments);
        if (end != null && end.isBefore(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS))) {
            return ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result);
        }
        return ok().body(result);
    }

    @GetMapping("request/jdbc/schema")
    public Collection<String> getRequestSchema(
            @RequestParam(name = "host") String host,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(name = "end") @Validate(Condition.INSTANT) Instant end)  {

        return requestService.getRequestSchema(environment, start, end, host);
    }

    @GetMapping("session/{sessionId}/user/action")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "user_action",
            view = USER_ACTION_ROW_MAPPER,
            select = "name,node_name,type,start,parent",
            order = "start")
    public Collection<UserAction> getUserActions(
            MvcRequest mvc,
            @PathVariable String sessionId) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.userAction().parent().eq(fromString(sessionId))); // UUID

        return (Collection<UserAction>) mvc.execute();
    }

    @GetMapping("session/user/{user}/action")
    @QueryExtension(select = REJECT, overrideView = false)
    @QueryTemplate(dataset = "main_session",
            view = USER_ACTION_ROW_MAPPER,
            select = "id,start:session_start,end,location,name:session_name,user_action.name:action_name,user_action.node_name,user_action.type,user_action.start:action_start",
            join = "user_action",
            order = "main_session.start,user_action.start", ignore = "date")
    public Collection<AnalyticDto> getUserActions(
            MvcRequest mvc,
            @PathVariable(name = "user") String user,
            @RequestParam(name = "date") @Validate(Condition.INSTANT) Instant date
    ) {
        var store = mvc.getStore().unwrap(InspectStore.class);
        mvc.getComposer().criteria(store.mainSession().user().eq(user).and(store.mainSession().start().ge(date)));

        return store.execute(mvc.getComposer().compose(store), rs -> {
            List<AnalyticDto> sessions = new ArrayList<>();
            while (rs.next()) {
                var userAction =  new UserAction(
                        rs.getString("action_name"),
                        rs.getString("nodeName"),
                        rs.getString("type"),
                        fromNullableTimestamp(rs.getTimestamp("action_start"))
                );
                var cdSession = rs.getString("id");
                var session = sessions.stream().filter(s -> s.getId().equals(cdSession)).findFirst().orElse(null);
                if(session == null) {
                    session = new AnalyticDto();
                    session.setId(rs.getObject("id", UUID.class));
                    session.setStart(fromNullableTimestamp(rs.getTimestamp("session_start")));
                    session.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                    session.setName(rs.getString("session_name"));
                    session.setLocation(rs.getString("location"));
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
        });
    }
}
