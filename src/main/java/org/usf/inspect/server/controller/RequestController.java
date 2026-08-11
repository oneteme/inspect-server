package org.usf.inspect.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import org.usf.inspect.server.validation.Condition;
import org.usf.inspect.server.validation.Validate;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.Keyword;
import org.usf.jquery.web.QueryRequestFilter;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.sql.Timestamp.from;
import static java.util.UUID.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.jquery.core.DBColumn.*;

/**
 * Legacy (v3) REST controller exposing read/query endpoints over inspect trace data:
 * instances, sessions, requests, request/session stages, exceptions, user actions and architecture views.
 */
@Slf4j
@CrossOrigin
@Validated
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final ObjectMapper mapper;

    /**
     * Retrieves the environment of a single instance.
     *
     * @param request the dynamic query built from the requested columns
     * @param idInstance the instance ID to fetch
     * @return the matching instance environment, cached for one hour
     */
    @GetMapping(value = "instance/{idInstance}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<InstanceEnvironment> getInstance(
       @QueryRequestFilter(view = "instance",
                           column = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,resource,configuration,id") QueryComposer request,
       @PathVariable String idInstance)  {
        return ok()
                .cacheControl(maxAge(1, HOURS))
                .body(INSPECT.execute(request.filters(column("id_ins").eq(fromString(idInstance))), InspectMappers.instanceEnvironmentMapper(mapper)));
    }

    // New
    /**
     * Retrieves the collector traces of a single instance.
     *
     * @param request the dynamic query built from the requested columns
     * @param idInstance the instance ID to fetch traces for
     * @return the list of instance traces
     */
    @GetMapping(value = "instance/{idInstance}/trace", produces = APPLICATION_JSON_VALUE)
    public List<InstanceTrace> getInstanceTraces(
            @QueryRequestFilter(view = "instance_trace",
                    column = "pending,attempts,size_session,filename,start,instance_env") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(column("cd_ins").eq(fromString(idInstance))), InspectMappers.instanceTraceMapper());
    }

    /**
     * Retrieves the machine resource usage history of a single instance.
     *
     * @param request the dynamic query built from the requested columns
     * @param idInstance the instance ID to fetch resource usages for
     * @return the list of resource usage measurements
     */
    @GetMapping(value = "instance/{idInstance}/resource/usage", produces = APPLICATION_JSON_VALUE)
    public List<MachineResourceUsage> getInstanceResourceUsages(
            @QueryRequestFilter(view = "resource_usage",
                    column = "low_heap,high_heap,start") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(column("cd_ins").eq(fromString(idInstance))), InspectMappers.instanceResourceUsageMapper());
    }

    /**
     * Retrieves the log entries of a single instance, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idInstance the instance ID to fetch log entries for
     * @return the list of log entries
     */
    @GetMapping(value = "instance/{idInstance}/log/entry", produces = APPLICATION_JSON_VALUE)
    public List<LogEntry> getLogEntries(
            @QueryRequestFilter(view = "log_entry",
                    column = "start,log_level,log_message,stacktrace", order = "start.desc") QueryComposer request,
            @PathVariable String idInstance)  {
        return INSPECT.execute(request.filters(column("cd_ins").eq(fromString(idInstance))), InspectMappers.instanceLogEntryMapper(mapper));
    }

    /**
     * Retrieves the distinct hosts contacted by requests of the given type within the given environment and period.
     *
     * @param type the request type (matching a {@link RequestType} name)
     * @param environment the environment to search into
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @return the distinct list of hosts
     * @throws IllegalArgumentException if {@code type} does not match a known {@link RequestType}
     */
    @GetMapping(value = "request/{type}/hosts", produces = APPLICATION_JSON_VALUE)
    public String[] getRequestHosts(
            @PathVariable String type,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(name = "end") @Validate(Condition.INSTANT) Instant end)  {
        TraceApiTable requestTable;
        try {
            requestTable = RequestType.valueOf(type).getTable();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return requestService.getRequestHosts(requestTable, environment, start, end);
    }
    /**
     * Retrieves the distinct database schemas used by JDBC requests for the given host, environment and period.
     *
     * @param host the database host to search into
     * @param environment the environment to search into
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @return the distinct list of schemas
     */
    @GetMapping(value = "request/jdbc/schema", produces = APPLICATION_JSON_VALUE)
    public String[] getRequestSchema(
            @RequestParam(name = "host") String host,
            @RequestParam(name = "env") String environment,
            @RequestParam(name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(name = "end") @Validate(Condition.INSTANT) Instant end)  {

        return requestService.getRequestSchema( environment, start, end, host);
    }
    /**
     * Searches REST requests matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param hosts the hosts to filter on
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @param rangestatus the status range codes to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the requests
     * @return the list of matching REST requests
     */
    @GetMapping(value = "request/rest", produces = APPLICATION_JSON_VALUE)
    public List<RestRequestDto> getRestRequests(@RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
                                                @RequestParam(required = false, name = "host") String[] hosts,
                                                @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
                                                @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
                                                @RequestParam(required = false, name = "rangestatus") String[] rangestatus,
                                                @RequestParam(required = false, name = "lazy") boolean lazy)  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(null, environments, null, start, end, null, null, hosts, null, null, null, null, null, null, null, rangestatus, lazy);
        return requestService.getRestRequests(jsf);
    }

    /**
     * Searches database (JDBC) requests matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param hosts the hosts to filter on
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @param rangestatus the failed/success range flags to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the requests
     * @return the list of matching database requests
     */
    @GetMapping(value = "request/database", produces = APPLICATION_JSON_VALUE)
    public List<DatabaseRequestDto> getDatabaseRequestForSearch(@RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
                                                                @RequestParam(required = false, name = "host") String[] hosts,
                                                                @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
                                                                @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
                                                                @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus,
                                                                @RequestParam(required = false, name = "lazy") boolean lazy
    )  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments, hosts, start, end, rangestatus, lazy);
        return requestService.getDatabaseRequests(jsf);
    }

    /**
     * Searches FTP requests matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param hosts the hosts to filter on
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @param rangestatus the failed/success range flags to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the requests
     * @return the list of matching FTP requests
     */
    @GetMapping(value = "request/ftp", produces = APPLICATION_JSON_VALUE)
    public List<FtpRequestDto> getFtpRequestForSearch(@RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
                                                      @RequestParam(required = false, name = "host") String[] hosts,
                                                      @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
                                                      @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
                                                      @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus,
                                                      @RequestParam(required = false, name = "lazy") boolean lazy
    )  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end, rangestatus, lazy);
        return requestService.getFtpRequests(jsf);
    }

    /**
     * Searches SMTP requests matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param hosts the hosts to filter on
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @param rangestatus the failed/success range flags to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the requests
     * @return the list of matching SMTP requests
     */
    @GetMapping(value = "request/smtp", produces = APPLICATION_JSON_VALUE)
    public List<MailRequestDto> getSmtpRequestForSearch(@RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
                                                        @RequestParam(required = false, name = "host") String[] hosts,
                                                        @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
                                                        @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
                                                        @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus,
                                                        @RequestParam(required = false, name = "lazy") boolean lazy
    )  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments,hosts,start,end, rangestatus, lazy);
        return requestService.getSmtpRequestsByFilter(jsf);
    }

    /**
     * Searches LDAP requests matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param hosts the hosts to filter on
     * @param start the lower bound (inclusive) of the request period
     * @param end the upper bound (inclusive) of the request period
     * @param rangestatus the failed/success range flags to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the requests
     * @return the list of matching LDAP requests
     */
    @GetMapping(value = "request/ldap", produces = APPLICATION_JSON_VALUE)
    public List<DirectoryRequestDto> getLdapRequestForSearch(@RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
                                                             @RequestParam(required = false, name = "host") String[] hosts,
                                                             @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
                                                             @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
                                                             @RequestParam(required = false, name = "rangestatus") Boolean[] rangestatus,
                                                             @RequestParam(required = false, name = "lazy") boolean lazy
    )  {
        JqueryRequestFilter jsf = new JqueryRequestFilter(environments, hosts, start, end, rangestatus, lazy);
        return requestService.getLdapRequestsByFilter(jsf);
    }

    /**
     * Retrieves the log entries of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param sessionId the session ID to fetch log entries for
     * @return the list of log entries
     */
    @GetMapping(value = "session/{sessionId}/log/entry", produces = APPLICATION_JSON_VALUE)
    public List<LogEntry> getLogEntriesBySessionId(
            @QueryRequestFilter(view = "log_entry",
                    column = "start,log_level,log_message,stacktrace", order = "start.desc") QueryComposer request,
            @PathVariable String sessionId)  {
        return INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(sessionId))), InspectMappers.instanceLogEntryMapper(mapper));
    }



    /**
     * Searches REST sessions matching the given filters.
     *
     * @param methods the HTTP methods to filter on
     * @param protocols the protocols to filter on
     * @param hosts the hosts to filter on
     * @param ports the ports to filter on
     * @param path the request path to filter on
     * @param query the request query string to filter on
     * @param medias the content types to filter on
     * @param auths the authentication schemes to filter on
     * @param status the HTTP status codes to filter on
     * @param start the lower bound (inclusive) of the session period
     * @param end the upper bound (inclusive) of the session period
     * @param apiNames the API names to filter on
     * @param users the users to filter on
     * @param appNames the application names to filter on
     * @param environments the environments to filter on, must not be empty if provided
     * @param rangestatus the status range codes to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the sessions
     * @return the list of matching REST sessions
     */
    @GetMapping(value = "session/rest", produces = APPLICATION_JSON_VALUE)
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
            @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
            @RequestParam(required = false, name = "apiname") String[] apiNames,
            @RequestParam(required = false, name = "user") String[] users,
            @RequestParam(required = false, name = "appname") String[] appNames,
            @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
            @RequestParam(required = false, name = "rangestatus") String[] rangestatus,
            @RequestParam(required = false, name = "lazy") boolean lazy
    )  {

        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(appNames, environments, users, start, end, methods, protocols, hosts, ports, medias, auths, status, apiNames, path, query,rangestatus, lazy);
        return requestService.getRestSessionsForSearch(jsf);
    }

    /**
     * Retrieves the REST sessions ("pulse" summary) of a single instance.
     *
     * @param request the dynamic query built from the requested columns
     * @param id the instance ID to fetch sessions for
     * @return the list of REST sessions
     */
    @GetMapping(value = "instance/{id}/session/rest", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RestSession>> getRestSessionsByInstance(
            @QueryRequestFilter(
                    view = "rest_session",
                    column = "id,api_name,method,host,path,status,start,end,thread,user",
                    order = "start,end") QueryComposer request,
            @PathVariable String id
    )  {
        return ok().body(INSPECT.execute(request.filters(column("cd_ins").eq(fromString(id))), InspectMappers.restSessionPulseRowMapper()));
    }

    /**
     * Retrieves the main sessions ("pulse" summary) of a single instance.
     *
     * @param request the dynamic query built from the requested columns
     * @param id the instance ID to fetch sessions for
     * @return the list of main sessions
     */
    @GetMapping(value = "instance/{id}/session/main", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MainSession>> getMainSessionsByInstance(
            @QueryRequestFilter(
                    view = "main_session",
                    column = "id,name,start,end,thread,type,location",
                    order = "start,end") QueryComposer request,
            @PathVariable String id
    )  {
        return ok().body(INSPECT.execute(request.filters(column("cd_ins").eq(fromString(id))), InspectMappers.mainSessionPulseRowMapper()));
    }

    /**
     * Retrieves the full details of a single REST session.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch
     * @return {@code 200 OK} with the session, or {@code 404 NOT_FOUND} if it does not exist
     * @throws SQLException if the underlying query fails
     */
    @GetMapping(value = "session/rest/{idSession}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RestSession> getRestSession(
            @QueryRequestFilter(view = "rest_session",
            column = "id,api_name,method,protocol,host,port,path,query,media,auth,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,err_type,err_msg,stacktrace,mask,user,user_agt,cache_control,linked,instance_env") QueryComposer request,
            @PathVariable String idSession) throws SQLException {
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_ses").eq(fromString(idSession))), InspectMappers.restSessionResultSetMapper(mapper)))
                .map(o -> ok().body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the stages of a single REST session.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch stages for
     * @return the list of session stages, ordered by their execution order
     */
    @GetMapping(value = "session/rest/{idSession}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HttpSessionStage>> getRestSessionStages (
            @QueryRequestFilter(view = "rest_session_stage",
                    column = "name,order,start,end",
                    order = "order") QueryComposer request,
            @PathVariable String idSession) {
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.restSessionStageMapper()));
    }

    /**
     * Retrieves the parent session/request identifiers of a given request or session.
     *
     * @param type the request type (matching a {@link RequestType} name)
     * @param id the identifier of the request/session to find the parent chain for
     * @return {@code 200 OK} with a map of type to parent ID, or {@code 404 NOT_FOUND} if no parent was found
     * @throws IllegalArgumentException if {@code type} does not match a known {@link RequestType}
     */
    @GetMapping(value = "{type}/{id}/parent", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getSessionParent(
            @PathVariable String type,
            @PathVariable String id
    )  {
        RequestType tableType;
        try {
            tableType = RequestType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid request type: " + type, e);
        }
        return Optional.of(requestService.getSessionParent(tableType, id))
                .filter(o -> !o.isEmpty())
                .map(o -> ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(o))
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the full main session tree (session, requests and stages) for the given session ID.
     *
     * @param id the main session ID, must be a valid UUID
     * @return {@code 200 OK} with the tree, cached for a day if completed, or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "session/main/{id}/tree", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Session> getMainTree(@PathVariable @Validate(Condition.UUID) String id)  {
        try {
            var result = requestService.getMainTree(id);
            return result.wasCompleted() ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result) : ok().body(result);
        } catch (NoSuchElementException e) {
            return status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Retrieves the full REST session tree (session, requests and stages) for the given session ID.
     *
     * @param id the REST session ID, must be a valid UUID
     * @return {@code 200 OK} with the tree, cached for a day if completed, or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "session/rest/{id}/tree", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Session> getRestTree(@PathVariable @Validate(Condition.UUID) String id)  {
        try {
            var result = requestService.getRestTree(id);
            return result.wasCompleted() ? ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result) : ok().body(result);
        } catch (NoSuchElementException e) {
            return status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Searches main sessions matching the given filters.
     *
     * @param environments the environments to filter on, must not be empty if provided
     * @param names the session names to filter on
     * @param launchModes the launch modes/types to filter on
     * @param location the location to filter on
     * @param start the lower bound (inclusive) of the session period
     * @param end the upper bound (inclusive) of the session period
     * @param users the users to filter on
     * @param appNames the application names to filter on
     * @param failed the failed/success flags to filter on
     * @param lazy whether to load a lightweight (lazy) representation of the sessions
     * @return the list of matching main sessions
     */
    @GetMapping(value = "session/main", produces = APPLICATION_JSON_VALUE) // can't optimise, done
    public List<MainSessionDto> getMainSessions(
            @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments,
            @RequestParam(required = false, name = "name") String[] names,
            @RequestParam(required = false, name = "launchmode") String[] launchModes,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
            @RequestParam(required = false, name = "user") String[] users,
            @RequestParam(required = false, name = "appname") String[] appNames,
            @RequestParam(required = false, name = "failed") Boolean[] failed,
            @RequestParam(required = false, name = "lazy") boolean lazy
    )  {

        JqueryMainSessionFilter fc = new JqueryMainSessionFilter(appNames, environments, users, start, end, names, launchModes, location, failed, lazy);
        return requestService.getMainSessionsForSearch(fc);
    }

    /**
     * Retrieves the full details of a single main session.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch
     * @return {@code 200 OK} with the session, or {@code 404 NOT_FOUND} if it does not exist
     * @throws SQLException if the underlying query fails
     */
    @GetMapping(value = "session/main/{idSession}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<MainSession> getMainSession(
            @QueryRequestFilter(
                view = "main_session",
                column = "id,name,start,end,type,location,thread,err_type,err_msg,stacktrace,mask,user,instance_env") QueryComposer request,
            @PathVariable String idSession) throws SQLException {
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_ses").eq(fromString(idSession))), InspectMappers.createBaseMainSession(mapper)))
                .map(o -> ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the REST requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of REST requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/rest", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RestRequestDto>>  getRestRequests(
            @QueryRequestFilter(
                view = "rest_request",
                column = "id,protocol,host,path,query,method,status,start,end,thread,user,body_content,linked,exception.err_type,exception.err_msg",
                join = "exception",
                order = "start") QueryComposer request,
            @PathVariable String  idSession){
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.restRequestLazyMapper()));
    }

    /**
     * Retrieves the exception details for the given list of request IDs.
     *
     * @param request the dynamic query built from the requested columns
     * @param idRequestList the request IDs to fetch exceptions for
     * @return a map of parent request ID to its exception information
     */
    @GetMapping(value = "session/request/exception", produces = APPLICATION_JSON_VALUE) // need to add exception type to front call
    public ResponseEntity<Map<Long, ExceptionInfo>> getRequestExceptions(
            @QueryRequestFilter(
                    view = "exception",
                    column = "err_type,err_msg,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam( name = "ids") String[] idRequestList)  {
        return ok().body(INSPECT.execute(request.filters(column("cd_rqt").in(Arrays.stream(idRequestList).map(UUID::fromString).toArray())), InspectMappers::exceptionInfoMapper));
    }

    /**
     * Retrieves the local (in-process) requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of local requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/local", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LocalRequest>> getLocalRequests(
            @QueryRequestFilter(
                    view = "local_request",
                    column = "id,name,location,start,end,user,thread,type,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession)  {
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.localRequestMapper()));
    }

    /**
     * Retrieves the database requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of database requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/database", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DatabaseRequestDto>> getDatabaseRequests(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,db,db_name,start,end,user,thread,command,schema,failed,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request, @PathVariable String idSession) {
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.databaseRequestLazyMapper()));
    }


    /**
     * Retrieves the full details of a single REST request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idRequest the request ID to fetch
     * @return {@code 200 OK} with the request (cached for 30 days if old enough), or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "request/rest/{idRequest}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<RestRequest> getRestRequest (
            @QueryRequestFilter(view = "rest_request",
                    column = "id,protocol,auth,host,port,path,query,method,status,size_in,size_out,content_encoding_in,content_encoding_out,start,end,thread,user,body_content,linked,instance_env,parent") QueryComposer request,
            @PathVariable String idRequest) {
        return  Optional.ofNullable(INSPECT.execute(request.filters(column("id_rst_rqt").eq(fromString(idRequest))), InspectMappers::restRequestMapperComplete))
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(()-> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the stages of a single REST request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idRequest the request ID to fetch stages for
     * @return the list of request stages, ordered by their execution order
     */
    @GetMapping(value = "request/rest/{idRequest}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<HttpRequestStage>> getRestRequestStages (
            @QueryRequestFilter(view = "rest_request_stage",
                    column = "name,order,start,end,exception.err_type,exception.err_msg,exception.stacktrace",
                    join = "exception",
                    order = "order") QueryComposer request,
            @PathVariable String idRequest) {
        return ok().body(INSPECT.execute(request.filters(column("cd_rst_rqt").eq(fromString(idRequest))), InspectMappers.restRequestStageMapper(mapper)));
    }

    /**
     * Retrieves the full details of a single database request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idDatabase the request ID to fetch
     * @return {@code 200 OK} with the request (cached for 30 days if old enough), or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "request/database/{idDatabase}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DatabaseRequest> getDatabaseRequest(
            @QueryRequestFilter(
                    view = "database_request",
                    column = "id,host,port,db,start,end,user,thread,driver,db_name,db_version,command,schema,failed,instance_env,parent") QueryComposer request, @PathVariable String idDatabase) {
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_dtb_rqt").eq(fromString(idDatabase))), InspectMappers::databaseRequestComplete))
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the stages of a single database request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idDatabase the request ID to fetch stages for
     * @return the list of request stages, ordered by their execution order
     */
    @GetMapping(value = "request/database/{idDatabase}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DatabaseRequestStage>> getDatabaseRequestStages(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "name,order,start,end,arg,action_count,command,exception.err_type,exception.err_msg,exception.stacktrace",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idDatabase) {
        return ok().body(INSPECT.execute(request.filters(column("cd_dtb_rqt").eq(fromString(idDatabase))), InspectMappers.databaseRequestStageMapper(mapper)));
    }

    /**
     * Computes the number of database action stages per parent request, for the given list of request IDs.
     *
     * @param request the dynamic query built from the requested columns
     * @param idDatabaseList the request IDs to compute stage counts for
     * @return a map of parent request ID to its action stage count
     */
    @GetMapping(value = "session/request/database/stages/count", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> getDatabaseRequestStagesCount(
            @QueryRequestFilter(
                    view = "database_stage",
                    column = "action_count,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idDatabaseList) {
        return ok().body(INSPECT.execute(request.filters(column("cd_dtb_rqt").in(Arrays.stream(idDatabaseList).map(UUID::fromString).toArray())), rs -> {
            Map<String, Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getString(PARENT.reference()), rs.getInt(ACTION_COUNT.reference()));
            }
            return actionsMap;
        }));
    }

    /**
     * Retrieves the FTP requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of FTP requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/ftp", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FtpRequestDto>> getFtpRequests(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,start,end,thread,user,command,failed,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return  ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.ftpRequestLazyMapper()));
    }

    /**
     * Retrieves the full details of a single FTP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idFtp the request ID to fetch
     * @return {@code 200 OK} with the request (cached for 30 days if old enough), or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "request/ftp/{idFtp}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<FtpRequest> getFtpRequest(
            @QueryRequestFilter(
                    view = "ftp_request",
                    column = "id,host,port,protocol,server_version,client_version,start,end,user,thread,command,failed,instance_env,parent") QueryComposer request,
            @PathVariable String idFtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_ftp_rqt").eq(fromString(idFtp))), InspectMappers::ftpRequestComplete))
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the stages of a single FTP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idFtp the request ID to fetch stages for
     * @return the list of request stages, ordered by their execution order
     */
    @GetMapping(value = "request/ftp/{idFtp}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FtpRequestStage>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,order,start,end,command,arg,exception.err_type,exception.err_msg,exception.stacktrace",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idFtp) {
        return ok().body(INSPECT.execute(request.filters(column("cd_ftp_rqt").eq(fromString(idFtp))), InspectMappers.ftpRequestStageMapper(mapper)));
    }

    /**
     * Retrieves, for the given list of FTP request IDs, the distinct stage names per parent request
     * (excluding CONNECTION/DISCONNECTION stages).
     *
     * @param request the dynamic query built from the requested columns
     * @param idFtpList the request IDs to fetch stage names for
     * @return a map of parent request ID to its list of stage names
     */
    @GetMapping(value = "session/request/ftp/stages", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,List<String>>> getFtpRequestStages(
            @QueryRequestFilter(
                    view = "ftp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return  ok().body(INSPECT.execute(request.filters(column("cd_ftp_rqt").in(Arrays.stream(idFtpList).map(UUID::fromString).toArray())
                                .and(FTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<String, List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getString(PARENT.reference()))){
                    actionsMap.put(rs.getString(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getString(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    /**
     * Retrieves the SMTP requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of SMTP requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/smtp", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MailRequestDto>> getSmtpRequests(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,start,end,thread,user,command,failed,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.smtpRequestLazyMapper()));
    }

    /**
     * Retrieves the full details of a single SMTP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSmtp the request ID to fetch
     * @return {@code 200 OK} with the request (cached for 30 days if old enough), or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "request/smtp/{idSmtp}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<MailRequest> getSmtpRequest(
            @QueryRequestFilter(
                    view = "smtp_request",
                    column = "id,host,port,start,end,user,thread,command,failed,instance_env,parent") QueryComposer request,
            @PathVariable String idSmtp){
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_smtp_rqt").eq(fromString(idSmtp))), InspectMappers::mailRequestCompleteMapper))
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }

    /**
     * Retrieves the stages of a single SMTP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSmtp the request ID to fetch stages for
     * @return the list of request stages, ordered by their execution order
     */
    @GetMapping(value = "request/smtp/{idSmtp}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MailRequestStage>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,order,start,command,end,exception.err_type,exception.err_msg,exception.stacktrace",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idSmtp) {
        return ok().body(INSPECT.execute(request.filters(column("cd_smtp_rqt").eq(fromString(idSmtp))), InspectMappers.mailRequestStageMapper(mapper)));
    }

    /**
     * Retrieves the mail contents (subject, from, recipients, etc.) sent by a single SMTP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSmtp the request ID to fetch mails for
     * @return the list of mails sent by the request
     */
    @GetMapping(value = "request/smtp/{idSmtp}/mail", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Mail>> getSmtpRequestMails(
            @QueryRequestFilter(
                    view = "smtp_mail",
                    column = "subject,from,recipients,media,reply_to,size") QueryComposer request, @PathVariable String idSmtp) {
        return ok().body(INSPECT.execute(request.filters(column("cd_smtp_rqt").eq(fromString(idSmtp))), InspectMappers.mailMapper()));
    }

    /**
     * Retrieves, for the given list of SMTP request IDs, the distinct stage names per parent request
     * (excluding CONNECTION/DISCONNECTION stages).
     *
     * @param request the dynamic query built from the requested columns
     * @param idSmtpList the request IDs to fetch stage names for
     * @return a map of parent request ID to its list of stage names
     */
    @GetMapping(value = "session/request/smtp/stages", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<String>>> getSmtpRequestStages(
            @QueryRequestFilter(
                    view = "smtp_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idSmtpList) {
        return ok().body(INSPECT.execute(request.filters(column("cd_smtp_rqt").in(Arrays.stream(idSmtpList).map(UUID::fromString).toArray()).and(SMTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<String, List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getString(PARENT.reference()))){
                    actionsMap.put(rs.getString(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getString(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    /**
     * Computes the number of mails sent per parent SMTP request, for the given list of request IDs.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSmtpList the request IDs to compute mail counts for
     * @return a map of parent request ID to its mail count
     */
    @GetMapping(value = "session/request/smtp/stages/count", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> getSmtpRequestStagesRowCount(
            @QueryRequestFilter(
                    view = "smtp_mail",
                    column = "parent.count:count,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idSmtpList) {
        return ok().body(INSPECT.execute(request.filters(column("cd_smtp_rqt").in(Arrays.stream(idSmtpList).map(UUID::fromString).toArray())), rs -> {
            Map<String, Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getString(PARENT.reference()), rs.getInt("count"));
            }
            return actionsMap;
        }));
    }


    /**
     * Retrieves the LDAP requests of a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch requests for
     * @return the list of LDAP requests belonging to the session
     */
    @GetMapping(value = "session/{idSession}/request/ldap", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DirectoryRequestDto>> getLdapRequests(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,start,end,thread,user,command,failed,exception.err_type,exception.err_msg",
                    join = "exception",
                    order = "start") QueryComposer request,
            @PathVariable String idSession){
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.ldapRequestLazyMapper()));
    }

    /**
     * Retrieves the full details of a single LDAP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idLdap the request ID to fetch
     * @return {@code 200 OK} with the request (cached for 30 days if old enough), or {@code 404 NOT_FOUND} if not found
     */
    @GetMapping(value = "request/ldap/{idLdap}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DirectoryRequest> getLdapRequest(
            @QueryRequestFilter(
                    view = "ldap_request",
                    column = "id,host,port,protocol,start,end,user,command,thread,failed,instance_env,parent") QueryComposer request,
            @PathVariable String idLdap){
        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_ldap_rqt").eq(fromString(idLdap))), InspectMappers::ldapRequestCompleteMapper))
                .map(o -> {
                    Instant end = o.getEnd();
                    boolean cacheable = end != null && Duration.between(Instant.now(), end).toDays() > 2;
                    return cacheable ? ok().cacheControl(maxAge(30, DAYS)).body(o) : ok().body(o);
                })
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
    }


    /**
     * Retrieves the stages of a single LDAP request.
     *
     * @param request the dynamic query built from the requested columns
     * @param idLdap the request ID to fetch stages for
     * @return the list of request stages, ordered by their execution order
     */
    @GetMapping(value = "request/ldap/{idLdap}/stage", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DirectoryRequestStage>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,order,start,end,command,arg,exception.err_type,exception.err_msg,exception.stacktrace",
                    join = "exception",
                    order = "order") QueryComposer request, @PathVariable String idLdap) {
        return ok().body(INSPECT.execute(request.filters(column("cd_ldap_rqt").eq(fromString(idLdap))), InspectMappers.ldapRequestStageMapper(mapper)));
    }

    /**
     * Retrieves, for the given list of LDAP request IDs, the distinct stage names per parent request
     * (excluding CONNECTION/DISCONNECTION stages).
     *
     * @param request the dynamic query built from the requested columns
     * @param idFtpList the request IDs to fetch stage names for
     * @return a map of parent request ID to its list of stage names
     */
    @GetMapping(value = "session/request/ldap/stages", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<String>>> getLdapRequestStages(
            @QueryRequestFilter(
                    view = "ldap_stage",
                    column = "name,parent",
                    ignoreParameters = "ids") QueryComposer request,
            @RequestParam(name = "ids") String[] idFtpList) {
        return ok().body(INSPECT.execute(request.filters(column("cd_ldap_rqt").in(Arrays.stream(idFtpList).map(UUID::fromString).toArray()).and(LDAP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION"))), rs -> {
            Map<String, List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getString(PARENT.reference()))){
                    actionsMap.put(rs.getString(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getString(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        }));
    }

    /**
     * Retrieves the user actions recorded within a single session, ordered by start date.
     *
     * @param request the dynamic query built from the requested columns
     * @param idSession the session ID to fetch user actions for
     * @return the list of user actions
     */
    @GetMapping(value = "session/{idSession}/user/action", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserAction>> getUserActions(
            @QueryRequestFilter(
                    view = "user_action",
                    column = "name,node_name,type,start,parent",
                    order = "start") QueryComposer request,
            @PathVariable String idSession) {
        return ok().body(INSPECT.execute(request.filters(column("cd_prn_ses").eq(fromString(idSession))), InspectMappers.userActionMapper()));
    }

    /**
     * Retrieves, for a single user, all main sessions and their user actions started on or after the given date.
     *
     * @param request the dynamic query built from the requested columns
     * @param user the user to fetch sessions and actions for
     * @param date the lower bound (inclusive) of the session start date
     * @return the list of sessions with their nested user actions
     */
    @GetMapping(value = "session/user/{user}/action", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalyticDto>> getUserActions(
            @QueryRequestFilter(
                    view = "main_session",
                    column = "id,start:session_start,end,location,name:session_name,user_action.name:action_name,user_action.node_name,user_action.type,user_action.start:action_start",
                    join = "user_action",
                    order = "main_session.start,user_action.start",
                    ignoreParameters = "date",
                    mergeParameters = {Keyword.LIMIT,Keyword.OFFSET}) QueryComposer request,
            @PathVariable(name = "user") String user,
            @RequestParam(name = "date") @Validate(Condition.INSTANT) Instant date
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

    /**
     * Builds the application architecture graph (instances and their connections) over the given period.
     *
     * @param start the lower bound (inclusive) of the period, may be {@code null}
     * @param end the upper bound (inclusive) of the period, may be {@code null}
     * @param environments the environments to filter on, must not be empty if provided
     * @return {@code 200 OK} with the architecture, cached for a day if the period ends in the past
     */
    @GetMapping(value = "architecture", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Architecture>> getArchitecture(
            @RequestParam(required = false, name = "start") @Validate(Condition.INSTANT) Instant start,
            @RequestParam(required = false, name = "end") @Validate(Condition.INSTANT) Instant end,
            @RequestParam(required = false, name = "env") @Validate(Condition.NOT_EMPTY) String[] environments
    )  {
        var result = requestService.createArchitecture(start, end, environments);
        if (end != null && end.isBefore(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS))) {
            return ok().cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)).body(result);
        }
        return ok().body(result);
    }
}
