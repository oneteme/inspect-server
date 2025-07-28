package org.usf.inspect.server.service;

import static java.sql.Timestamp.from;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.inspect.server.config.constant.JoinConstant.*;
import static org.usf.jquery.core.Mappers.toArray;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.usf.inspect.core.*;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.dto.DtoRequest;
import org.usf.inspect.server.dto.DtoRestRequest;
import org.usf.inspect.server.exception.PayloadTooLargeException;
import org.usf.inspect.server.mapper.InspectMappers;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.ColumnProxy;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.JDBCType;
import org.usf.jquery.core.NamedColumn;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.core.SingleQueryColumn;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.core.ViewJoin;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Service
@RequiredArgsConstructor
@Setter
public class RequestService {

    private final JdbcTemplate template;
    private final RequestDao dao;
    private int requestLimit = 300000;

    public void addInstance(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    public void updateInstance(Instant end,String instanceId){
        dao.updateInstanceEnvironment(end, instanceId);
    }

    public void addInstanceTrace(InstanceTrace instanceTrace){
        dao.saveInstanceTrace(instanceTrace);
    }

    @TraceableStage
    @Transactional(rollbackFor = Throwable.class)
    public long addEventTraces(List<EventTrace> eventTraces) {
        return dao.saveTraceables(eventTraces);
    }

    public Session getMainTree(String id)  {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds); // todo : optimise
        Session session = getMainSessionForTree(id);
        if(session != null){
            prntIncList.add(session);
        }
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    public Session getRestTree(String id)  {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds); // todo : optimise
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    private void createTree(List<Session> sessions) {
        sessions.forEach(prntA ->
                sessions.forEach(prntB -> {
                    if (!Objects.equals(prntA.getId(), prntB.getId())){
                        Optional<RestRequestWrapper> opt = prntB.getRestRequests() != null ? prntB.getRestRequests().stream()
                                .filter(k -> prntA.getId().equals(k.getId()))
                                .findFirst() : Optional.empty();
                        if (opt.isPresent()) {
                            var ex = opt.get();
                            ex.setRemoteTrace((RestSessionWrapper) prntA);
                        }
                    }
                })
        );
    }

    public List<Architecture> createArchitecture(Instant start, Instant end, String[] env){
        var v = new QueryComposer()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(DATABASE_REQUEST.column(DB).as("name"), DATABASE_REQUEST.column(SCHEMA).as("schema"))
                .columns(DBColumn.constant("JDBC").as("type"))
                .distinct(true)
                .joins(REST_SESSION.join(DATABASE_REQUEST_JOIN))
                .joins(REST_SESSION.join(INSTANCE_JOIN))
                .filters(DATABASE_REQUEST.column(DB).notNull().or(DATABASE_REQUEST.column(SCHEMA).notNull()))
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v2 = new QueryComposer()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(FTP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("FTP").as("type"))
                .distinct(true)
                .joins(REST_SESSION.join(FTP_REQUEST_JOIN))
                .joins(REST_SESSION.join(INSTANCE_JOIN))
                .filters(FTP_REQUEST.column(HOST).notNull())
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v3 = new QueryComposer()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(SMTP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("SMTP").as("type"))
                .distinct(true)
                .joins(REST_SESSION.join("request"))
                .joins(REST_SESSION.join(INSTANCE_JOIN))
                .filters(SMTP_REQUEST.column(HOST).notNull())
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v4 = new QueryComposer()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(LDAP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("LDAP").as("type"))
                .distinct(true)
                .joins(REST_SESSION.join(LDAP_REQUEST_JOIN))
                .joins(REST_SESSION.join(INSTANCE_JOIN))
                .filters(LDAP_REQUEST.column(HOST).notNull())
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v5 = v.toString() + " UNION " + v2.toString() + " UNION " + v3.toString() + " UNION " + v4.toString();
        Object[] args = new Object[]{from(start), from(end), String.join(",", env), from(start), from(end), String.join(",", env), from(start), from(end), String.join(",", env), from(start), from(end), String.join(",", env)};
        int[] argTypes = new int[]{TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR};
        return template.query(v5, args, argTypes, rs -> {
            Map<String, List<Architecture>> map = new HashMap<>();
            while(rs.next()) {
                var key = rs.getString(APP_NAME.reference());
                if(!map.containsKey(key)) {
                    map.put(key, new ArrayList<>());
                }
                map.get(key).add(new Architecture(rs.getString("name"), rs.getString("schema"), rs.getString("type"), null));
            }
            return map.entrySet().stream().map(entry -> new Architecture(entry.getKey(), null, "REST", entry.getValue())).toList();
        });
    }

    public Map<String,String> getSessionParent(String childId){
        var prnt = getPropertyByFilters(REST_REQUEST, PARENT, REST_REQUEST.column(REMOTE).eq(childId));
        if(prnt != null){
            var res = getPropertyByFilters(REST_SESSION, ID, REST_SESSION.column(ID).eq(prnt));
            if(res != null) {
                return Map.of("id", res, "type", "rest");
            }
            res = getPropertyByFilters(MAIN_SESSION, ID, MAIN_SESSION.column(ID).eq(prnt));
            if(res!= null){
                var type = getPropertyByFilters(MAIN_SESSION, TYPE, MAIN_SESSION.column(ID).eq(res));
                return Map.of("id", res, "type", type);
            }
        }
        return Collections.emptyMap();
    }


    public List<Session> getRestSessionsForTree(List<String> ids)  {
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getRestSessions(jsf, true);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().stream().toList();
            getRestRequestsCompleteForParent(parentIds).forEach(r -> reqMap.get(r.getSessionId()).getRestRequests().add(r));
            getDatabaseRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getSessionId()).getDatabaseRequests().add(q));
            getFtpRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getSessionId()).getFtpRequests().add(q));
            getSmtpRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getSessionId()).getMailRequests().add(q));
            getLdapRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getSessionId()).getLdapRequests().add(q));
        }
        return sessions;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public List<Session> getRestSessionsForSearch(JqueryRequestSessionFilter jsf) {

        var count = getRestSessionCountForSearch(jsf);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(
                        getColumns(
                                REST_SESSION, ID, API_NAME, METHOD,
                                PROTOCOL, PATH, QUERY, STATUS, SIZE_IN, SIZE_OUT,
                                START, END, USER, ERR_TYPE, ERR_MSG
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
//                .joins(REST_SESSION.join(INSTANCE_JOIN))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
      return INSPECT.execute(v, InspectMappers.restSessionShallowMapper());
    }

    public int getRestSessionCountForSearch(JqueryRequestSessionFilter jsf) {

        var v = new QueryComposer()
                .columns(REST_SESSION.column(INSTANCE_ENV).count().as("count"));
        if (jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
            v.filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }
        return INSPECT.execute(v, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    public List<Session> getRestSessionsForDump(String env, String appName, Instant start, Instant end) {
        var cte = new QueryComposer()
                .filters(INSTANCE.column(START).le(from(end)))
                .columns(getColumns(INSTANCE, ID, START))
                .filters(INSTANCE.column(START).le(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).eq(env))
                .filters(INSTANCE.column(APP_NAME).eq(appName)).compose();
        var v = new QueryComposer()
                .ctes(cte)
                .columns(
                    getColumns(
                        REST_SESSION, ID, API_NAME, METHOD,
                        PROTOCOL, PATH, QUERY, STATUS, SIZE_IN, SIZE_OUT,
                        START, END, USER, THREAD, HOST, ERR_MSG, ERR_TYPE
                    )
                )
                .filters(REST_SESSION.column(END).ge(from(start)).and(REST_SESSION.column(START).le(from(end))))
                .filters(REST_SESSION.column(INSTANCE_ENV).in(new QueryComposer().columns(new ViewColumn("id", cte, JDBCType.VARCHAR, null)).compose().asColumn()))
                .orders(REST_SESSION.column(START).order());
        return INSPECT.execute(v, InspectMappers.restSessionDumpMapper());
    }


    public List<Session> getRestSessions(JqueryRequestSessionFilter jsf, boolean lazy)  { // remove if possible after optimizing tree
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            REST_SESSION, ID, API_NAME, METHOD,
                            PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT,
                            START, END, THREAD, ERR_TYPE, ERR_MSG, MASK, USER, USER_AGT, CACHE_CONTROL, INSTANCE_ENV
                    ));
        if(lazy) {
            v.columns(getColumns(INSTANCE, APP_NAME, OS, RE, ADDRESS)).filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
        return INSPECT.execute(v, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                RestSessionWrapper session = new RestSessionWrapper();
                session.setId(rs.getString(ID.reference()));
                session.setMethod(rs.getString(METHOD.reference()));
                session.setProtocol(rs.getString(PROTOCOL.reference()));
                session.setHost(rs.getString(HOST.reference()));
                session.setPort(rs.getInt(PORT.reference()));
                session.setPath(rs.getString(PATH.reference()));
                session.setQuery(rs.getString(QUERY.reference()));
                session.setContentType((rs.getString(MEDIA.reference())));
                session.setAuthScheme((rs.getString(AUTH.reference())));
                session.setStatus(rs.getInt(STATUS.reference()));
                session.setInDataSize(rs.getLong(SIZE_IN.reference()));
                session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                session.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                session.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setThreadName(rs.getString(THREAD.reference()));
                //session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUserAgent(rs.getString(USER_AGT.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                session.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                if(lazy) {
                    session.setOs(rs.getString(OS.reference()));
                    session.setRe(rs.getString(RE.reference()));
                    session.setAddress(rs.getString(ADDRESS.reference()));
                    session.setAppName(rs.getString(APP_NAME.reference()));
                }
                session.setMask(rs.getInt(MASK.reference()));
                if(RequestMask.JDBC.is(session.getMask())) {
                    session.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(session.getMask())) {
                    session.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(session.getMask())) {
                    session.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(session.getMask())) {
                    session.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(session.getMask())) {
                    session.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(session.getMask())) {
                    session.setLdapRequests(new ArrayList<>());
                }
                sessions.add(session);
            }
            return sessions;
        });
    }

    public Session getMainSessionForTree(String id)  {
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        Session session = requireSingle(getMainSessions(jsf, true));
        if (session != null) {
            getRestRequestsCompleteForParent(Collections.singletonList(session.getId())).forEach(r -> session.getRestRequests().add(r));
            getDatabaseRequestsComplete(session.getId()).forEach(r -> session.getDatabaseRequests().add(r));
            getFtpRequestsComplete(session.getId()).forEach(r -> session.getFtpRequests().add(r));
            getSmtpRequestsComplete(session.getId()).forEach(r -> session.getMailRequests().add(r));
            getLdapRequestsComplete(session.getId()).forEach(r -> session.getLdapRequests().add(r));
        }
        return session;
    }

    public List<Session> getMainSessionsForDump(String env, String appName, Instant start, Instant end) {

        var cte = new QueryComposer()
                .filters(INSTANCE.column(START).le(from(end)))
                .columns(getColumns(INSTANCE, ID, START))
                .filters(INSTANCE.column(START).le(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).eq(env))
                .filters(INSTANCE.column(APP_NAME).eq(appName)).compose();
        var v = new QueryComposer()
                .ctes(cte)
                .columns(
                        getColumns(
                                MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD, ERR_TYPE, ERR_MSG
                        )
                )
                .filters(MAIN_SESSION.column(END).ge(from(start)).and(MAIN_SESSION.column(START).le(from(end))))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).in(new QueryComposer().columns(new ViewColumn("id", cte, JDBCType.VARCHAR, null)).compose().asColumn()))
                .orders(MAIN_SESSION.column(START).order());
        return INSPECT.execute(v, InspectMappers.mainSessionDumpMapper());
    }

    public Session getMainSession(String id){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getMainSessions(jsf, false));
    }

    /**
     * @deprecated
     */
    @Deprecated
    public List<Session> getMainSessionsForSearch(JqueryMainSessionFilter jsf) {

        var count = getMainSessionCountForSearch(jsf);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(
                        getColumns(
                                MAIN_SESSION, ID, NAME, START, END, LOCATION, TYPE,
                                USER,ERR_TYPE, ERR_MSG
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return INSPECT.execute(v, InspectMappers.mainSessionForSearchMapper());
    }

    public int getMainSessionCountForSearch(JqueryMainSessionFilter jsf) {
        var v = new QueryComposer()
                .columns(MAIN_SESSION.column(INSTANCE_ENV).count().as("count"));
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
            v.filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }
        return INSPECT.execute(v, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    public List<Session> getMainSessions(JqueryMainSessionFilter jsf, boolean lazy) {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD,
                            ERR_TYPE, ERR_MSG, MASK, USER, INSTANCE_ENV
                    ));
        if(lazy) {
            v.columns(getColumns(INSTANCE, APP_NAME, OS, RE, ADDRESS)).filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }

        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return INSPECT.execute(v, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                MainSessionWrapper main = new MainSessionWrapper();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                //main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                if(lazy) {
                    main.setAppName(rs.getString(APP_NAME.reference()));
                    main.setOs(rs.getString(OS.reference()));
                    main.setRe(rs.getString(RE.reference()));
                    main.setAddress(rs.getString(ADDRESS.reference()));
                }
                main.setUser(rs.getString(USER.reference()));
                main.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                main.setMask(rs.getInt(MASK.reference()));
                if(RequestMask.JDBC.is(main.getMask())) {
                    main.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(main.getMask())) {
                    main.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(main.getMask())) {
                    main.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(main.getMask())) {
                    main.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(main.getMask())) {
                    main.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(main.getMask())) {
                    main.setLdapRequests(new ArrayList<>());
                }
                sessions.add(main);
            }
            return sessions;
        });
    }

    public List<RestRequestWrapper> getRestRequestsCompleteForParent(List<String> cdSession)  {
        return getRestRequestsCompleteByFilters(new DBFilter[]{REST_REQUEST.column(PARENT).in(cdSession.toArray())});
    }



    public List<DtoRestRequest> getRestRequestsLazyForSearch(JqueryRequestSessionFilter jsf)  { // garbage
        List<DtoRestRequest> mergeList= new ArrayList<>();

        List<DBFilter> restFilters = (ArrayList)jsf.filters(REST_REQUEST);
        restFilters.add(REST_SESSION.column(START).ge(from(jsf.getStart())));
        restFilters.add(REST_SESSION.column(START).lt(from(jsf.getEnd())));

        mergeList.addAll(getRestRequestsByFilter(
                restFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{
                        REST_REQUEST.join(EXCEPTION_JOIN),
                        REST_REQUEST.join(REST_SESSION_JOIN),
                        REST_SESSION.join(INSTANCE_JOIN)
                },
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}
        ));

        List<DBFilter> mainFilters = (ArrayList)jsf.filters(REST_REQUEST);
        mainFilters.add(MAIN_SESSION.column(START).ge(from(jsf.getStart())));
        mainFilters.add(MAIN_SESSION.column(START).lt(from(jsf.getEnd())));

        mergeList.addAll(getRestRequestsByFilter(
                mainFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{
                        REST_REQUEST.join(EXCEPTION_JOIN),
                        REST_REQUEST.join(MAIN_SESSION_JOIN),
                        MAIN_SESSION.join(INSTANCE_JOIN)
                },
                "main",
                getColumns(MAIN_SESSION, TYPE)
        ));
        return  mergeList;
    }

    public RestRequestWrapper getRestRequestsCompleteById(int cdSession)  {
        return requireSingle(getRestRequestsCompleteByFilters(new DBFilter[]{REST_REQUEST.column(ID).eq(cdSession)}));
    }

    private List<RestRequestWrapper> getRestRequestsCompleteByFilters(DBFilter[] filters)  { //use criteria
        var v = new QueryComposer()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, AUTH, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN))
                .filters(filters)
                .orders(REST_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<RestRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestWrapper out = new RestRequestWrapper();
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setId(rs.getString(ID.reference()));
                out.setId(rs.getString(REMOTE.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setPath(rs.getString(PATH.reference()));
                out.setQuery(rs.getString(QUERY.reference()));
                out.setMethod(rs.getString(METHOD.reference()));
                out.setStatus(rs.getInt(STATUS.reference()));
                out.setInDataSize(rs.getLong(SIZE_IN.reference()));
                out.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                out.setInContentEncoding(rs.getString(CONTENT_ENCODING_IN.reference()));
                out.setOutContentEncoding(rs.getString(CONTENT_ENCODING_OUT.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setAuthScheme(rs.getString(AUTH.reference()));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private int getRequestCountByTable(TraceApiTable table ,DBFilter[] filters, ViewJoin[][] joins){
        var v = new QueryComposer()
                .columns(table.column(PARENT).count().as("count"))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters);
        return INSPECT.execute(v, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    private List<DtoRestRequest> getRestRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType)  { //use criteria

        var count = getRequestCountByTable(REST_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PATH, QUERY, METHOD, STATUS, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(REST_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DtoRestRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRestRequest out = new DtoRestRequest();
                out.setId(rs.getString(ID.reference()));
                out.setId(rs.getString(REMOTE.reference()));
                out.setParent(rs.getString(PARENT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPath(rs.getString(PATH.reference()));
                out.setQuery(rs.getString(QUERY.reference()));
                out.setMethod(rs.getString(METHOD.reference()));
                out.setStatus(rs.getInt(STATUS.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                out.setAppName(rs.getString(APP_NAME.reference()));
                outs.add(out);
            }
            return outs;
        });
    }


    public DatabaseRequestWrapper getDatabaseRequestComplete(long idDatabase)  {
        return requireSingle(getDatabaseRequestsComplete(DATABASE_REQUEST.column(ID).eq(idDatabase)));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequestsComplete(List<String> cdSession)  {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequestsComplete(String cdSession)  {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getDatabaseRequestsLazyForSearch(JqueryRequestFilter jsf) {
        List<DtoRequest> mergeList= new ArrayList<>();

        List<DBFilter> restFilters = (ArrayList)jsf.filters(DATABASE_REQUEST);
        restFilters.add(REST_SESSION.column(START).ge(from(jsf.getStart())));
        restFilters.add(REST_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getDatabaseRequestsByFilter(restFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{DATABASE_REQUEST.join(EXCEPTION_JOIN), DATABASE_REQUEST.join(REST_SESSION_JOIN),REST_SESSION.join(INSTANCE_JOIN)},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));

        List<DBFilter> mainFilters = (ArrayList)jsf.filters(DATABASE_REQUEST);
        mainFilters.add(MAIN_SESSION.column(START).ge(from(jsf.getStart())));
        mainFilters.add(MAIN_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getDatabaseRequestsByFilter(mainFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{DATABASE_REQUEST.join(EXCEPTION_JOIN),DATABASE_REQUEST.join(MAIN_SESSION_JOIN),MAIN_SESSION.join(INSTANCE_JOIN)},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    private List<DtoRequest> getDatabaseRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type,NamedColumn[] mainType)  {
        var count = getRequestCountByTable(DATABASE_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                DATABASE_REQUEST, ID, HOST ,DB, START, END, THREAD, COMMAND, FAILED, SCHEMA, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(DATABASE_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setCommand(rs.getString(COMMAND.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<DatabaseRequestWrapper> getDatabaseRequestsComplete(DBFilter filter)  {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            DATABASE_REQUEST, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                            DB_NAME, DB_VERSION, COMMAND, FAILED, SCHEMA, PARENT
                    ))
                .filters(filter)
                .orders(DATABASE_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper();
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setDriverVersion(rs.getString(DRIVER.reference()));
                out.setProductName(rs.getString(DB_NAME.reference()));
                out.setProductVersion(rs.getString(DB_VERSION.reference()));
                out.setActions(new ArrayList<>());
                //out.setCommand(rs.getString(COMMAND.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                outs.add(out);
            }
            return outs;
        });
    }


    public FtpRequestWrapper getFtpRequestComplete(long id) {
        return requireSingle(getFtpRequestsComplete(FTP_REQUEST.column(ID).eq(id)));
    }

    public List<FtpRequestWrapper> getFtpRequestsComplete(List<String> cdSession)  {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<FtpRequestWrapper> getFtpRequestsComplete(String cdSession) {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).eq(cdSession));
    }


    public List<DtoRequest> getFtpRequestsLazyForSearch(JqueryRequestFilter jsf) {
        List<DtoRequest> mergeList= new ArrayList<>();

        List<DBFilter> restFilters = (ArrayList)jsf.filters(FTP_REQUEST);
        restFilters.add(REST_SESSION.column(START).ge(from(jsf.getStart())));
        restFilters.add(REST_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getFtpRequestsByFilter(restFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{FTP_REQUEST.join(EXCEPTION_JOIN),FTP_REQUEST.join(REST_SESSION_JOIN),REST_SESSION.join(INSTANCE_JOIN)},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));

        List<DBFilter> mainFilters = (ArrayList)jsf.filters(FTP_REQUEST);
        mainFilters.add(MAIN_SESSION.column(START).ge(from(jsf.getStart())));
        mainFilters.add(MAIN_SESSION.column(START).lt(from(jsf.getEnd())));
       mergeList.addAll(getFtpRequestsByFilter(mainFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{FTP_REQUEST.join(EXCEPTION_JOIN),FTP_REQUEST.join(MAIN_SESSION_JOIN),MAIN_SESSION.join(INSTANCE_JOIN)},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    public List<DtoRequest>getFtpRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType)  {
        var count = getRequestCountByTable(FTP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                FTP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        )
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(FTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<FtpRequestWrapper> getFtpRequestsComplete(DBFilter filter) {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            FTP_REQUEST, ID, HOST, PORT, PROTOCOL, SERVER_VERSION, CLIENT_VERSION, START, END, USER, THREAD, FAILED, PARENT
                    )
                )
                .filters(filter)
                .orders(FTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<FtpRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequestWrapper out = new FtpRequestWrapper();
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setServerVersion(rs.getString(SERVER_VERSION.reference()));
                out.setClientVersion(rs.getString(CLIENT_VERSION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean(FAILED.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public MailRequestWrapper getSmtpRequestsComplete(long id) {
        return requireSingle(getSmtpRequestsComplete(SMTP_REQUEST.column(ID).eq(id)));
    }

    public List<MailRequestWrapper> getSmtpRequestsComplete(List<String> cdSession) {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<MailRequestWrapper> getSmtpRequestsComplete(String cdSession) {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getSmtpRequestsLazyForSearch(JqueryRequestFilter jsf) {
        List<DtoRequest> mergeList= new ArrayList<>();

        List<DBFilter> restFilters = (ArrayList)jsf.filters(SMTP_REQUEST);
        restFilters.add(REST_SESSION.column(START).ge(from(jsf.getStart())));
        restFilters.add(REST_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getSmtpRequestsByFilter(restFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{SMTP_REQUEST.join(EXCEPTION_JOIN),SMTP_REQUEST.join(REST_SESSION_JOIN),REST_SESSION.join(INSTANCE_JOIN)},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));

        List<DBFilter> mainFilters = (ArrayList)jsf.filters(SMTP_REQUEST);
        mainFilters.add(MAIN_SESSION.column(START).ge(from(jsf.getStart())));
        mainFilters.add(MAIN_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getSmtpRequestsByFilter(mainFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{SMTP_REQUEST.join(EXCEPTION_JOIN),SMTP_REQUEST.join(MAIN_SESSION_JOIN),MAIN_SESSION.join(INSTANCE_JOIN)},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    public List<DtoRequest> getSmtpRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType)  {
        var count = getRequestCountByTable(SMTP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                SMTP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(SMTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<MailRequestWrapper> getSmtpRequestsComplete(DBFilter filter) {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            SMTP_REQUEST, ID, HOST, PORT, START, END, USER, THREAD, FAILED, PARENT
                    ))
                .filters(filter)
                .orders(SMTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<MailRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequestWrapper out = new MailRequestWrapper();
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean(FAILED.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public NamingRequestWrapper getLdapRequestsComplete(long id) {
        return requireSingle(getLdapRequestsComplete(LDAP_REQUEST.column(ID).eq(id)));
    }

    public List<NamingRequestWrapper> getLdapRequestsComplete(List<String> cdSession)  {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<NamingRequestWrapper> getLdapRequestsComplete(String cdSession)  {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).eq(cdSession));
    }


    public List<DtoRequest> getLdapRequestsLazyForSearch(JqueryRequestFilter jsf) {
        List<DtoRequest> mergeList= new ArrayList<>();

        List<DBFilter> restFilters = (ArrayList)jsf.filters(LDAP_REQUEST);
        restFilters.add(REST_SESSION.column(START).ge(from(jsf.getStart())));
        restFilters.add(REST_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getLdapRequestsByFilter(restFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{LDAP_REQUEST.join(EXCEPTION_JOIN),LDAP_REQUEST.join(REST_SESSION_JOIN),REST_SESSION.join(INSTANCE_JOIN)},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));

        List<DBFilter> mainFilters = (ArrayList)jsf.filters(LDAP_REQUEST);
        mainFilters.add(MAIN_SESSION.column(START).ge(from(jsf.getStart())));
        mainFilters.add(MAIN_SESSION.column(START).lt(from(jsf.getEnd())));
        mergeList.addAll(getLdapRequestsByFilter(mainFilters.toArray(DBFilter[]::new),
                new ViewJoin[][]{LDAP_REQUEST.join(EXCEPTION_JOIN),LDAP_REQUEST.join(MAIN_SESSION_JOIN),MAIN_SESSION.join(INSTANCE_JOIN)},
          "main",
                getColumns(MAIN_SESSION,TYPE)));

        return  mergeList;
    }

    public List<DtoRequest> getLdapRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType)  {
        var count = getRequestCountByTable(LDAP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                LDAP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(LDAP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                //out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<NamingRequestWrapper> getLdapRequestsComplete(DBFilter filter) {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            LDAP_REQUEST, ID, HOST, PORT, PROTOCOL, START, END, USER, THREAD, FAILED, PARENT
                    ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<NamingRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                NamingRequestWrapper out = new NamingRequestWrapper();
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean(FAILED.reference()));
                outs.add(out);
            }
            return outs;
        });
    }


    public String[] getRequestsHostsByType(String  type, String environement, Instant start, Instant end){
        var table = TraceApiTable.valueOf(RequestType.valueOf(type).getTable());
        var mask = RequestMask.valueOf(type.toUpperCase()).getValue();
        var v1 = new QueryComposer()
                .distinct(true)
                .columns(table.column(HOST))
        		.subViewQuery(INSTANCE.view(), sub-> sub.filters(INSTANCE.column(ENVIRONEMENT).eq(environement)))
                .filters(table.column(START).ge(from(start)))
                .filters(table.column(START).lt(from(end)))
                .filters(table.column(PARENT).in(sessionIdByTypeEnvironementPeriod("rest_session", start, end, mask)))
                .unions(new QueryComposer()
                        .distinct(true)
                        .columns(getColumns(table,HOST))
                        .filters(table.column(START).ge(from(start)))
                        .filters(table.column(START).lt(from(end)))
                        .filters(table.column(PARENT).in(sessionIdByTypeEnvironementPeriod("main_session", start, end, mask)))
                        .compose().asUnion(true));

        return INSPECT.execute(v1, toArray(rs -> rs.getString(HOST.reference()), String[]::new));
    }

    public SingleQueryColumn sessionIdByTypeEnvironementPeriod(String type, Instant start, Instant end, int mask){
        var table = TraceApiTable.valueOf(RequestType.valueOf(type).getTable());
        var v  = new QueryComposer()
                .columns(getColumns(table,ID))
                .joins(table.join(INSTANCE_JOIN))
                .filters(table.column(START).ge(from(start)),
                        table.column(START).lt(from(end))
                       /* ,table.column(MASK).bitAnd(mask).gt(0)*/
                );
//        if(Objects.equals(type, "rest_session")){
//            v.filters(INSTANCE.column(TYPE).eq("SERVER"));
//        }
        return v.compose().asColumn();
    }

    private String getPropertyByFilters(TraceApiTable table, TraceApiColumn target, DBFilter filters) { // main / apissesion
        var v = new QueryComposer().columns(getColumns(table,target)).filters(filters);
        return INSPECT.execute(v, rs -> {
            if(rs.next()){
                return rs.getString(target.reference()); // to be changed
            }
            return null;
        });
    }

    private static NamedColumn[] getColumns(ViewDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(NamedColumn[]::new);
    }

    public static ExceptionInfo getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message, stackTraceRows, null);
        }
        return null;
    }

    private static <T extends Enum<T>> T valueOfNullable(Class<T> classe, String value) {
        return ofNullable(value)
                .flatMap(v -> Stream.of(classe.getEnumConstants()).filter(e -> e.name().equals(v)).findAny())
                .orElse(null);
    }

    public static <T extends Enum<T>> List<T> valueOfNullabletoEnumList(Class<T> classe, String values){
        return Stream.of(splitNullable(values))
                .map(v-> valueOfNullable(classe, v))
                .filter(Objects::nonNull)
                .toList();
    }

    private static final String[] empty_array = new String[0];

    private static String[] splitNullable(String s){
        return isNull(s) ? empty_array : s.split(",");
    }
}
