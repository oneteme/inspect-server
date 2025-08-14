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
import static org.usf.inspect.server.mapper.InspectMappers.*;
import static org.usf.jquery.core.Mappers.toArray;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.usf.inspect.core.*;
import org.usf.inspect.core.RequestMask;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.dto.*;
import org.usf.inspect.server.exception.PayloadTooLargeException;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.*;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final JdbcTemplate template;
    private final RequestDao dao;
    private final int requestLimit = 300000;

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
        var prnt = getPropertyByFilters(REST_REQUEST, PARENT, REST_REQUEST.column(ID).varchar().eq(childId));
        if(prnt != null){
            var res = getPropertyByFilters(REST_SESSION, ID, REST_SESSION.column(ID).varchar().eq(prnt));
            if(res != null) {
                return Map.of("id", res, "type", "rest");
            }
            res = getPropertyByFilters(MAIN_SESSION, ID, MAIN_SESSION.column(ID).varchar().eq(prnt));
            if(res!= null){
                var type = getPropertyByFilters(MAIN_SESSION, TYPE, MAIN_SESSION.column(ID).varchar().eq(res));
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
    public List<RestSessionDto> getRestSessionsForSearch(JqueryRequestSessionFilter jsf) {

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
                .joins(REST_SESSION.join(INSTANCE_JOIN));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
      return INSPECT.execute(v, restSessionShallowMapper());
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

    public List<RestSession> getRestSessionsForDump(String env, String appName, Instant start, Instant end) {
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
        return INSPECT.execute(v, restSessionDumpMapper());
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
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
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
                session.setRequestsMask(rs.getInt(MASK.reference()));
                if(RequestMask.JDBC.is(session.getRequestsMask())) {
                    session.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(session.getRequestsMask())) {
                    session.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(session.getRequestsMask())) {
                    session.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(session.getRequestsMask())) {
                    session.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(session.getRequestsMask())) {
                    session.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(session.getRequestsMask())) {
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

    public List<MainSession> getMainSessionsForDump(String env, String appName, Instant start, Instant end) {

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
        return INSPECT.execute(v, mainSessionDumpMapper());
    }

    /**
     * @deprecated
     */
    @Deprecated
    public List<MainSessionDto> getMainSessionsForSearch(JqueryMainSessionFilter jsf) {

        var count = getMainSessionCountForSearch(jsf);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(
                        getColumns(
                                MAIN_SESSION, ID, NAME, START, END, LOCATION, TYPE,
                                USER, ERR_TYPE, ERR_MSG
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return INSPECT.execute(v, mainSessionForSearchMapper());
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
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                if(lazy) {
                    main.setAppName(rs.getString(APP_NAME.reference()));
                    main.setOs(rs.getString(OS.reference()));
                    main.setRe(rs.getString(RE.reference()));
                    main.setAddress(rs.getString(ADDRESS.reference()));
                }
                main.setUser(rs.getString(USER.reference()));
                main.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                main.setRequestsMask(rs.getInt(MASK.reference()));
                if(RequestMask.JDBC.is(main.getRequestsMask())) {
                    main.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(main.getRequestsMask())) {
                    main.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(main.getRequestsMask())) {
                    main.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(main.getRequestsMask())) {
                    main.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(main.getRequestsMask())) {
                    main.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(main.getRequestsMask())) {
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
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                outs.add(out);
            }
            return outs;
        });
    }

    private int getRequestCountByTable(TraceApiTable table, DBFilter[] filters){
        var v = new QueryComposer()
                .columns(table.column(PARENT).count().as("count"))
                .joins(table.join(INSTANCE_JOIN))
                .filters(filters);
        return INSPECT.execute(v, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    public List<RestRequestDto> getRestRequests(JqueryRequestSessionFilter jsf)  {
        var filters = jsf.filters(REST_REQUEST).toArray(DBFilter[]::new);
        var count = getRequestCountByTable(REST_REQUEST, filters);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PATH, QUERY, METHOD, STATUS, START, END, THREAD, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN))
                .joins(REST_REQUEST.join(INSTANCE_JOIN))
                .filters(filters)
                .orders(REST_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<RestRequestDto> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestDto out = new RestRequestDto();
                out.setId(rs.getString(ID.reference()));
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPath(rs.getString(PATH.reference()));
                out.setQuery(rs.getString(QUERY.reference()));
                out.setMethod(rs.getString(METHOD.reference()));
                out.setStatus(rs.getInt(STATUS.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<DatabaseRequestWrapper> getDatabaseRequestsComplete(List<String> cdSession)  {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequestsComplete(String cdSession)  {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DatabaseRequestDto> getDatabaseRequests(JqueryRequestFilter jsf)  {
        var filters = jsf.filters(DATABASE_REQUEST).toArray(DBFilter[]::new);
        var count = getRequestCountByTable(DATABASE_REQUEST, filters);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(
                        getColumns(
                                DATABASE_REQUEST, ID, HOST ,DB, START, END, THREAD, COMMAND, FAILED, SCHEMA, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(DATABASE_REQUEST.join(EXCEPTION_JOIN))
                .joins(DATABASE_REQUEST.join(INSTANCE_JOIN))
                .filters(filters)
                .orders(DATABASE_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DatabaseRequestDto> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestDto out = new DatabaseRequestDto();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setCommand(rs.getString(COMMAND.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
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
                out.setCommand(rs.getString(COMMAND.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<FtpRequestWrapper> getFtpRequestsComplete(List<String> cdSession)  {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<FtpRequestWrapper> getFtpRequestsComplete(String cdSession) {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<FtpRequestDto> getFtpRequests(JqueryRequestFilter jsf)  {
        var filters = jsf.filters(FTP_REQUEST).toArray(DBFilter[]::new);
        var count = getRequestCountByTable(FTP_REQUEST, filters);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(
                        getColumns(
                                FTP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        )
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(FTP_REQUEST.join(EXCEPTION_JOIN))
                .joins(FTP_REQUEST.join(INSTANCE_JOIN))
                .filters(filters)
                .orders(FTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<FtpRequestDto> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequestDto out = new FtpRequestDto();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
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

    public List<MailRequestWrapper> getSmtpRequestsComplete(List<String> cdSession) {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<MailRequestWrapper> getSmtpRequestsComplete(String cdSession) {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<MailRequestDto> getSmtpRequestsByFilter(JqueryRequestFilter jsf)  {
        var filters = jsf.filters(SMTP_REQUEST).toArray(DBFilter[]::new);
        var count = getRequestCountByTable(SMTP_REQUEST, filters);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryComposer()
                .columns(
                        getColumns(
                                SMTP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(SMTP_REQUEST.join(EXCEPTION_JOIN))
                .joins(SMTP_REQUEST.join(INSTANCE_JOIN))
                .filters(filters)
                .orders(SMTP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<MailRequestDto> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequestDto out = new MailRequestDto();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
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

    public List<DirectoryRequestWrapper> getLdapRequestsComplete(List<String> cdSession)  {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DirectoryRequestWrapper> getLdapRequestsComplete(String cdSession)  {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DirectoryRequestDto> getLdapRequestsByFilter(JqueryRequestFilter jsf)  {
        var filters = jsf.filters(LDAP_REQUEST).toArray(DBFilter[]::new);
        var count = getRequestCountByTable(LDAP_REQUEST,filters);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryComposer()
                .columns(
                        getColumns(
                                LDAP_REQUEST, ID, HOST, START, END, THREAD, FAILED, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LDAP_REQUEST.join(EXCEPTION_JOIN))
                .joins(LDAP_REQUEST.join(INSTANCE_JOIN))
                .filters(filters)
                .orders(LDAP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DirectoryRequestDto> outs = new ArrayList<>();
            while (rs.next()) {
                DirectoryRequestDto out = new DirectoryRequestDto();
                out.setId(rs.getString(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setFailed(rs.getBoolean(FAILED.reference()));
                out.setSessionId(rs.getString(PARENT.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference()), null));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<DirectoryRequestWrapper> getLdapRequestsComplete(DBFilter filter) {
        var v = new QueryComposer()
                .columns(
                    getColumns(
                            LDAP_REQUEST, ID, HOST, PORT, PROTOCOL, START, END, USER, THREAD, FAILED, PARENT
                    ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return INSPECT.execute(v, rs -> {
            List<DirectoryRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DirectoryRequestWrapper out = new DirectoryRequestWrapper();
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

    public String[] getRequestHosts(String  type, String environment, Instant start, Instant end){
        var table = TraceApiTable.valueOf(RequestType.valueOf(type).getTable());
        var v1 = new QueryComposer()
                .distinct(true)
                .columns(table.column(HOST))
                .joins(table.join(INSTANCE_JOIN))
                .filters(table.column(START).ge(from(start)))
                .filters(table.column(START).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).eq(environment));
        return INSPECT.execute(v1, toArray(rs -> rs.getString(HOST.reference()), String[]::new));
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
