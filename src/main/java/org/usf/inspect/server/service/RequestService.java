package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.exception.PayloadTooLargeException;
import org.usf.inspect.server.mapper.MainSessionForSearchMapper;
import org.usf.inspect.server.mapper.RestSessionForSearchMapper;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.dto.DtoRequest;
import org.usf.inspect.server.dto.DtoRestRequest;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.jquery.core.*;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import static java.sql.Timestamp.from;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.server.RequestType.*;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.inspect.server.config.constant.JoinConstant.*;

@Service
@RequiredArgsConstructor
@Setter
@ConfigurationProperties(prefix = "inspect")
public class RequestService {

    private final JdbcTemplate template;
    private final DataSource ds;
    private final RequestDao dao;
    private int requestLimit= 300000;

    public void addInstance(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    public void updateInstance(Instant end,String instanceId){
        dao.updateInstanceEnvironment(end,instanceId);
    }

    @TraceableStage
    @Transactional(rollbackFor = Throwable.class)
    public long addSessions(List<Session> sessions) {
        return dao.saveSessions(sessions);
    }

    public Session getMainTree(String id) throws SQLException {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds);
        Session session = getMainSessionForTree(id);
        if(session != null){
            prntIncList.add(session);
        }
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    public Session getRestTree(String id) throws SQLException {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds);
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    private void createTree(List<Session> sessions) {
        sessions.forEach(prntA ->
                sessions.forEach(prntB -> {
                    if (!Objects.equals(prntA.getId(), prntB.getId())){
                        Optional<RestRequest> opt = prntB.getRestRequests() != null ? prntB.getRestRequests().stream()
                                .filter(k -> prntA.getId().equals(k.getId()))
                                .findFirst() : Optional.empty();
                        if (opt.isPresent()) {
                            var ex = opt.get();
                            ex.setRemoteTrace((RestSession) prntA);
                        }
                    }
                })
        );
    }

    public List<Architecture> createArchitecture(Instant start, Instant end, String[] env) throws SQLException {
        var v = new QueryBuilder()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(DATABASE_REQUEST.column(DB).as("name"), DATABASE_REQUEST.column(SCHEMA).as("schema"))
                .columns(DBColumn.constant("JDBC").as("type"))
                .distinct()
                .joins(REST_SESSION.join(DATABASE_REQUEST_JOIN).build())
                .joins(REST_SESSION.join(INSTANCE_JOIN).build())
                .filters(DATABASE_REQUEST.column(DB).notNull().or(DATABASE_REQUEST.column(SCHEMA).notNull()))
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v2 = new QueryBuilder()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(FTP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("FTP").as("type"))
                .distinct()
                .joins(REST_SESSION.join(FTP_REQUEST_JOIN).build())
                .joins(REST_SESSION.join(INSTANCE_JOIN).build())
                .filters(FTP_REQUEST.column(HOST).notNull())
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v3 = new QueryBuilder()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(SMTP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("SMTP").as("type"))
                .distinct()
                .joins(REST_SESSION.join(SMTP_REQUEST_JOIN).build())
                .joins(REST_SESSION.join(INSTANCE_JOIN).build())
                .filters(SMTP_REQUEST.column(HOST).notNull())
                .filters(REST_SESSION.column(START).ge(from(start)))
                .filters(REST_SESSION.column(END).lt(from(end)))
                .filters(INSTANCE.column(ENVIRONEMENT).in(env));
        var v4 = new QueryBuilder()
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(LDAP_REQUEST.column(HOST).as("name"))
                .columns(DBColumn.constant(null).as("schema"))
                .columns(DBColumn.constant("LDAP").as("type"))
                .distinct()
                .joins(REST_SESSION.join(LDAP_REQUEST_JOIN).build())
                .joins(REST_SESSION.join(INSTANCE_JOIN).build())
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

    public Map<String,String> getSessionParent(String childId) throws SQLException{
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

    public InstanceEnvironment getInstance(String id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                INSTANCE, ID, USER, TYPE, START, END, APP_NAME, VERSION, ADDRESS,
                                ENVIRONEMENT, OS, RE, COLLECTOR, BRANCH, HASH
                        ))
                .filters(INSTANCE.column(ID).eq(id));
        return v.build().execute(ds, rs -> {
            if(rs.next()) {
                var instanceEnvironment = new InstanceEnvironment(
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference()),
                        rs.getString(USER.reference()),
                        InstanceType.valueOf(rs.getString(TYPE.reference())),
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        rs.getString(COLLECTOR.reference()),
                        rs.getString(BRANCH.reference()),
                        rs.getString(HASH.reference()),
                        fromNullableTimestamp(rs.getTimestamp(END.reference())));

                instanceEnvironment.setId(rs.getString(ID.reference()));
                return instanceEnvironment;
            }
            return null;
        });
    }

    public Session getRestSession(String id) throws SQLException{
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getRestSessions(jsf, false));
    }

    public List<Session> getRestSessionsForTree(List<String> ids) throws SQLException {
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getRestSessions(jsf, true);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().stream().toList();
            getRestRequestsCompleteForParent(parentIds).forEach(r -> reqMap.get(r.getCdSession()).getRestRequests().add(r));
            getDatabaseRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getCdSession()).getDatabaseRequests().add(q));
            getFtpRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getCdSession()).getFtpRequests().add(q));
            getSmtpRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getCdSession()).getMailRequests().add(q));
            getLdapRequestsComplete(parentIds).forEach(q -> reqMap.get(q.getCdSession()).getLdapRequests().add(q));
        }
        return sessions;
    }

    @Deprecated
    public List<Session> getRestSessionsForSearch(JqueryRequestSessionFilter jsf) throws SQLException {

        var count = getRestSessionCountForSearch(jsf);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                REST_SESSION, ID, API_NAME, METHOD,
                                PROTOCOL, PATH, QUERY, STATUS, SIZE_IN, SIZE_OUT,
                                START, END, USER, ERR_TYPE, ERR_MSG
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
      return v.build().execute(ds, new RestSessionForSearchMapper());
    }

    public int getRestSessionCountForSearch(JqueryRequestSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(REST_SESSION.column(INSTANCE_ENV).count().as("count"));
        if (jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
            v.filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }
        return v.build().execute(ds, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    public List<Session> getRestSessionsForDump(String env, String appName, Instant start, Instant end) throws SQLException {
        var cte = new QueryBuilder()
                .columns(getColumns(INSTANCE, ID, START))
                .filters(INSTANCE.column(ENVIRONEMENT).eq(env))
                .filters(INSTANCE.column(APP_NAME).eq(appName)).asView();
        var v = new QueryBuilder()
                .ctes(cte)
                .columns(
                    getColumns(
                        REST_SESSION, ID, API_NAME, METHOD,
                        PROTOCOL, PATH, QUERY, STATUS, SIZE_IN, SIZE_OUT,
                        START, END, USER, THREAD, HOST, ERR_MSG, ERR_TYPE
                    )
                )
                .filters(REST_SESSION.column(START).ge(new QueryBuilder().columns(new ViewColumn("start", cte, JDBCType.TIMESTAMP, null).max().as("dh_max")).asView().asColumn()))
                .filters(REST_SESSION.column(END).ge(from(start)).and(REST_SESSION.column(START).le(from(end))))
                .filters(REST_SESSION.column(INSTANCE_ENV).in(new QueryBuilder().columns(new ViewColumn("id", cte, JDBCType.VARCHAR, null)).asView().asColumn()))
                .orders(REST_SESSION.column(START).order());
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                RestSession session = new RestSession();
                session.setId(rs.getString(ID.reference()));
                session.setMethod(rs.getString(METHOD.reference()));
                session.setProtocol(rs.getString(PROTOCOL.reference()));
                session.setPath(rs.getString(PATH.reference()));
                session.setQuery(rs.getString(QUERY.reference()));
                session.setStatus(rs.getInt(STATUS.reference()));
                session.setInDataSize(rs.getLong(SIZE_IN.reference()));
                session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setHost(rs.getString(HOST.reference()));
                session.setThreadName(rs.getString(THREAD.reference()));
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                sessions.add(session);
            }
            return sessions;
        });
    }


    public List<Session> getRestSessions(JqueryRequestSessionFilter jsf, boolean lazy) throws SQLException {
        var v = new QueryBuilder()
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
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                RestSession session = new RestSession();
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
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
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

    public Session getMainSessionForTree(String id) throws SQLException {
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

    public List<Session> getMainSessionsForDump(String env, String appName, Instant start, Instant end) throws SQLException {

        var cte = new QueryBuilder()
                .columns(getColumns(INSTANCE, ID, START))
                .filters(INSTANCE.column(ENVIRONEMENT).eq(env))
                .filters(INSTANCE.column(APP_NAME).eq(appName)).asView();
        var v = new QueryBuilder()
                .ctes(cte)
                .columns(
                        getColumns(
                                MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD, ERR_TYPE, ERR_MSG
                        )
                )
                .filters(MAIN_SESSION.column(START).ge(new QueryBuilder().columns(new ViewColumn("start", cte, JDBCType.TIMESTAMP, null).max().as("dh_max")).asView().asColumn()))
                .filters(MAIN_SESSION.column(END).ge(from(start)).and(MAIN_SESSION.column(START).le(from(end))))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).in(new QueryBuilder().columns(new ViewColumn("id", cte, JDBCType.VARCHAR, null)).asView().asColumn()))
                .orders(MAIN_SESSION.column(START).order());
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                MainSession main = new MainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                sessions.add(main);
            }
            return sessions;
        });
    }

    public Session getMainSession(String id) throws SQLException{
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getMainSessions(jsf, false));
    }

    @Deprecated
    public List<Session> getMainSessionsForSearch(JqueryMainSessionFilter jsf) throws SQLException {

        var count = getMainSessionCountForSearch(jsf);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
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
        return v.build().execute(ds, new MainSessionForSearchMapper());
    }

    public int getMainSessionCountForSearch(JqueryMainSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(MAIN_SESSION.column(INSTANCE_ENV).count().as("count"));
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
            v.filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        }
        return v.build().execute(ds, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    public List<Session> getMainSessions(JqueryMainSessionFilter jsf, boolean lazy) throws SQLException {
        var v = new QueryBuilder()
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
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                MainSession main = new MainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
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

    public List<RestRequest> getRestRequestsCompleteForParent(List<String> cdSession) throws SQLException {
        return getRestRequestsCompleteByFilters(new DBFilter[]{REST_REQUEST.column(PARENT).in(cdSession.toArray())});
    }

    public List<RestRequest> getRestRequestsLazyForParent(String cdSession) throws SQLException {
        return getRestRequestsLazy(Collections.singletonList(cdSession));
    }

    public List<DtoRestRequest> getRestRequestsLazyForSearch(JqueryRequestSessionFilter jsf) throws SQLException {
        List<DtoRestRequest> mergeList= new ArrayList<>();
        mergeList.addAll(getRestRequestsByFilter(jsf.filters(REST_REQUEST).toArray(DBFilter[]::new),
                                                 new ViewJoin[][]{REST_REQUEST.join(EXCEPTION_JOIN).build(),REST_REQUEST.join(REST_SESSION_JOIN).build(),REST_SESSION.join(INSTANCE_JOIN).build()}));
        mergeList.addAll(getRestRequestsByFilter(jsf.filters(REST_REQUEST).toArray(DBFilter[]::new),
                                                 new ViewJoin[][]{REST_REQUEST.join(EXCEPTION_JOIN).build(),REST_REQUEST.join(MAIN_SESSION_JOIN).build(),MAIN_SESSION.join(INSTANCE_JOIN).build()}));
        return  mergeList;
    }

    public RestRequest getRestRequestsCompleteById(int cdSession) throws SQLException {
        return requireSingle(getRestRequestsCompleteByFilters(new DBFilter[]{REST_REQUEST.column(ID).eq(cdSession)}));
    }

    private List<RestRequest> getRestRequestsCompleteByFilters(DBFilter[] filters) throws SQLException { //use criteria
        var v = new QueryBuilder()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, AUTH, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(filters)
                .orders(REST_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<RestRequest> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequest out = new RestRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
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
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<RestRequest> getRestRequestsLazy(List<String> cdSessions) throws SQLException { //use criteria
        var v = new QueryBuilder()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PATH, QUERY, METHOD, STATUS, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(REST_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(REST_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(REST_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<RestRequest> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequest out = new RestRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setId(rs.getString(REMOTE.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPath(rs.getString(PATH.reference()));
                out.setQuery(rs.getString(QUERY.reference()));
                out.setMethod(rs.getString(METHOD.reference()));
                out.setStatus(rs.getInt(STATUS.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private int getRequestCountByTable(TraceApiTable table ,DBFilter[] filters, ViewJoin[][] joins) throws SQLException {
        var v = new QueryBuilder()
                .columns(table.column(ID).count().as("count"))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters);
        return v.build().execute(ds, rs -> {
            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        });
    }

    private List<DtoRestRequest> getRestRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins) throws SQLException { //use criteria

        var count = getRequestCountByTable(REST_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }
        var v = new QueryBuilder()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PATH, QUERY, METHOD, STATUS, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(REST_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DtoRestRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRestRequest out = new DtoRestRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setId(rs.getString(REMOTE.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPath(rs.getString(PATH.reference()));
                out.setQuery(rs.getString(QUERY.reference()));
                out.setMethod(rs.getString(METHOD.reference()));
                out.setStatus(rs.getInt(STATUS.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                out.setAppName(rs.getString(APP_NAME.reference()));
                outs.add(out);
            }
            return outs;
        });
    }



    /*private  <T> List<T> getObjectbyFilter (DBFilter[] filters, ViewJoin[][] joins){

    }*/



    public Map<Long, ExceptionInfo> getRestRequestExceptions(Long[] ids) throws SQLException{
        return this.getSubRequestExceptions(EXCEPTION.column(PARENT).in(ids).and(EXCEPTION.column(TYPE).eq(REST.name())));
    }

    public Map<Long, ExceptionInfo> getSubRequestExceptions(DBFilter filter) throws SQLException{
        var v = new QueryBuilder()
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG, PARENT))
                .filters(filter);
        return v.build().execute(ds, rs -> {
            Map<Long,ExceptionInfo> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()),new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
            }
            return actionsMap;
        });
    }

    public List<LocalRequest> getLocalRequests(String cdSession) throws SQLException{
        return getLocalRequests(Collections.singletonList(cdSession));
    }

    private List<LocalRequest> getLocalRequests(List<String> cdSessions) throws SQLException{
        var v = new QueryBuilder()
                .columns(getColumns(
                        LOCAL_REQUEST, ID, NAME, LOCATION, START, END, USER, THREAD, STATUS, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LOCAL_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(LOCAL_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(LOCAL_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<LocalRequest> outs = new ArrayList<>();
            while (rs.next()) {
                LocalRequest out = new LocalRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }


    public DatabaseRequest getDatabaseRequestComplete(long idDatabase) throws SQLException {
        return requireSingle(getDatabaseRequestsComplete(DATABASE_REQUEST.column(ID).eq(idDatabase)));
    }

    public List<DatabaseRequest> getDatabaseRequestsComplete(List<String> cdSession) throws SQLException {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DatabaseRequest> getDatabaseRequestsComplete(String cdSession) throws SQLException {
        return getDatabaseRequestsComplete(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DatabaseRequest> getDatabaseRequestsLazy(String cdSession) throws SQLException {
        return getDatabaseRequestsLazy(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getDatabaseRequestsLazyForSearch(JqueryRequestFilter jsf) throws SQLException{// todo: fix this
        List<DtoRequest> mergeList= new ArrayList<>();
        mergeList.addAll(getDatabaseRequestsByFilter(jsf.filters(DATABASE_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{DATABASE_REQUEST.join(EXCEPTION_JOIN).build(),DATABASE_REQUEST.join(REST_SESSION_JOIN).build(),REST_SESSION.join(INSTANCE_JOIN).build()},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));
        mergeList.addAll(getDatabaseRequestsByFilter(jsf.filters(DATABASE_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{DATABASE_REQUEST.join(EXCEPTION_JOIN).build(),DATABASE_REQUEST.join(MAIN_SESSION_JOIN).build(),MAIN_SESSION.join(INSTANCE_JOIN).build()},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    private List<DtoRequest> getDatabaseRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type,NamedColumn[] mainType) throws SQLException {
        var count = getRequestCountByTable(DATABASE_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                DATABASE_REQUEST, ID, HOST ,DB, START, END, THREAD, COMMAND, STATUS, SCHEMA, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(DATABASE_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setCommand(rs.getString(COMMAND.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<DatabaseRequest> getDatabaseRequestsComplete(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            DATABASE_REQUEST, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                            DB_NAME, DB_VERSION, COMMAND, STATUS, SCHEMA, PARENT
                    ))
                .filters(filter)
                .orders(DATABASE_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequest out = new DatabaseRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
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
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                outs.add(out);
            }
            return outs;
        });
    }



    private List<DatabaseRequest> getDatabaseRequestsLazy(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                DATABASE_REQUEST, ID, HOST ,DB, START, END, USER, THREAD, COMMAND, STATUS, SCHEMA
                        ))
                .filters(filter)
                .orders(DATABASE_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequest out = new DatabaseRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setName(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setCommand(rs.getString(COMMAND.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setSchema(rs.getString(SCHEMA.reference()));
                outs.add(out);
            }
            return outs;
        });
    }
    public Map<Long,Integer>  getDatabaseRequestStageRowCount(Long[] ids) throws SQLException {
        return getDatabaseRequestStageRowCount(DATABASE_STAGE.column(PARENT).in(ids));
    }

    public Map<Long, Integer> getDatabaseRequestStageRowCount(DBFilter filter) throws SQLException{
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                DATABASE_STAGE, ACTION_COUNT, PARENT
                        ))
                .filters(filter);
        return v.build().execute(ds, rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt(ACTION_COUNT.reference()));
            }
            return actionsMap;
        });
    }

    public Map<Long, ExceptionInfo> getDatabaseRequestExceptions(Long[] ids) throws SQLException{
        return this.getSubRequestExceptions(EXCEPTION.column(PARENT).in(ids).and(EXCEPTION.column(TYPE).eq(JDBC.name())));
    }


    public List<DatabaseRequestStage> getDatabaseRequestStages(Long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                DATABASE_STAGE, NAME, START, END, ACTION_COUNT, COMMANDS, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(DATABASE_STAGE.join(EXCEPTION_JOIN).build())
                .filters(DATABASE_STAGE.column(PARENT).eq(id))
                .orders(DATABASE_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestStage> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new DatabaseRequestStage();
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
                action.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(COMMANDS.reference())).toArray(new SqlCommand[0]));
                action.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public FtpRequest getFtpRequestComplete(long id) throws SQLException {
        return requireSingle(getFtpRequestsComplete(FTP_REQUEST.column(ID).eq(id)));
    }

    public List<FtpRequest> getFtpRequestsComplete(List<String> cdSession) throws SQLException {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<FtpRequest> getFtpRequestsComplete(String cdSession) throws SQLException {
        return getFtpRequestsComplete(FTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<FtpRequest> getFtpRequestsLazy(String cdSession) throws SQLException {
        return getFtpRequestsLazy(FTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getFtpRequestsLazyForSearch(JqueryRequestFilter jsf) throws SQLException{
        List<DtoRequest> mergeList= new ArrayList<>();
        mergeList.addAll(getFtpRequestsByFilter(jsf.filters(FTP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{FTP_REQUEST.join(EXCEPTION_JOIN).build(),FTP_REQUEST.join(REST_SESSION_JOIN).build(),REST_SESSION.join(INSTANCE_JOIN).build()},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));
        mergeList.addAll(getFtpRequestsByFilter(jsf.filters(FTP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{FTP_REQUEST.join(EXCEPTION_JOIN).build(),FTP_REQUEST.join(MAIN_SESSION_JOIN).build(),MAIN_SESSION.join(INSTANCE_JOIN).build()},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    public List<DtoRequest>getFtpRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType) throws SQLException {
        var count = getRequestCountByTable(FTP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                FTP_REQUEST, ID, HOST, START, END, THREAD, STATUS, PARENT
                        )
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(FTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<FtpRequest> getFtpRequestsComplete(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            FTP_REQUEST, ID, HOST, PORT, PROTOCOL, SERVER_VERSION, CLIENT_VERSION, START, END, USER, THREAD, STATUS, PARENT
                    )
                )
                .filters(filter)
                .orders(FTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequest> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequest out = new FtpRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
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
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<FtpRequest> getFtpRequestsLazy(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                FTP_REQUEST, ID, HOST, START, END, THREAD, STATUS
                        )
                )
                .filters(filter)
                .orders(FTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequest> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequest out = new FtpRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public Map<Long, List<String>> getFtpRequestStages(Long[] ids) throws SQLException{
        return getFtpRequestStages(FTP_STAGE.column(PARENT).in(ids));
    }

    public Map<Long, List<String>> getFtpRequestStages(DBFilter filter) throws  SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                FTP_STAGE, NAME, PARENT
                        ))
                .filters(filter.and(FTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION")));
        return v.build().execute(ds, rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        });
    }

    public Map<Long, ExceptionInfo> getFtpRequestExceptions(Long[] ids) throws SQLException{
        return this.getSubRequestExceptions(EXCEPTION.column(PARENT).in(ids).and(EXCEPTION.column(TYPE).eq(FTP.name())));
    }


    public List<FtpRequestStage> getFtpRequestStages(long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            FTP_STAGE, NAME, START, END, ARG, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(FTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(FTP_STAGE.column(PARENT).eq(id))
                .orders(FTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequestStage> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new FtpRequestStage();
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                action.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public MailRequest getSmtpRequestsComplete(long id) throws SQLException {
        return requireSingle(getSmtpRequestsComplete(SMTP_REQUEST.column(ID).eq(id)));
    }

    public List<MailRequest> getSmtpRequestsComplete(List<String> cdSession) throws SQLException {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<MailRequest> getSmtpRequestsComplete(String cdSession) throws SQLException {
        return getSmtpRequestsComplete(SMTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<MailRequest> getSmtpRequestsLazy(String cdSession) throws SQLException {
        return getSmtpRequestsLazy(SMTP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getSmtpRequestsLazyForSearch(JqueryRequestFilter jsf) throws SQLException{
        List<DtoRequest> mergeList= new ArrayList<>();
        mergeList.addAll(getSmtpRequestsByFilter(jsf.filters(SMTP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{SMTP_REQUEST.join(EXCEPTION_JOIN).build(),SMTP_REQUEST.join(REST_SESSION_JOIN).build(),REST_SESSION.join(INSTANCE_JOIN).build()},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));
        mergeList.addAll(getSmtpRequestsByFilter(jsf.filters(SMTP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{SMTP_REQUEST.join(EXCEPTION_JOIN).build(),SMTP_REQUEST.join(MAIN_SESSION_JOIN).build(),MAIN_SESSION.join(INSTANCE_JOIN).build()},
                "main",
                getColumns(MAIN_SESSION,TYPE)));
        return  mergeList;
    }

    public List<DtoRequest> getSmtpRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType) throws SQLException {
        var count = getRequestCountByTable(SMTP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                SMTP_REQUEST, ID, HOST, START, END, THREAD, STATUS, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(SMTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<MailRequest> getSmtpRequestsComplete(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            SMTP_REQUEST, ID, HOST, PORT, START, END, USER, THREAD, STATUS, PARENT
                    ))
                .filters(filter)
                .orders(SMTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<MailRequest> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequest out = new MailRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<MailRequest> getSmtpRequestsLazy(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                SMTP_REQUEST, ID, HOST, START, END, THREAD, STATUS
                        ))
                .filters(filter)
                .orders(SMTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<MailRequest> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequest out = new MailRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<MailRequestStage> getSmtpRequestStages(long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(SMTP_STAGE, NAME, START, END, PARENT)
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(SMTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(SMTP_STAGE.column(PARENT).eq(id))
                .orders(SMTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<MailRequestStage> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new MailRequestStage();
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public Map<Long, List<String>> getSmtpRequestStages(Long[] ids) throws SQLException{
        return this.getSmtpRequestStages(SMTP_STAGE.column(PARENT).in(ids));
    }

    public Map<Long, List<String>>  getSmtpRequestStages( DBFilter filter) throws SQLException{
        var v = new QueryBuilder()
                .columns(
                        getColumns(SMTP_STAGE, NAME, PARENT)
                )
                .filters(filter.and(SMTP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION")));
        return v.build().execute(ds, rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        });
    }

    public Map<Long, Integer> getSmtpRequestStageRowCount( Long[] ids) throws SQLException{
        return this.getSmtpRequestStageRowCount(SMTP_MAIL.column(PARENT).in(ids));
    }

    public Map<Long, Integer> getSmtpRequestStageRowCount( DBFilter filter) throws SQLException{
        var v = new QueryBuilder()
                .columns(SMTP_MAIL.column(PARENT).count().as("count"))
                .columns(SMTP_MAIL.column(PARENT))
                .filters(filter);
        return v.build().execute(ds, rs -> {
            Map<Long,Integer> actionsMap= new HashMap<>();
            while (rs.next()) {
                actionsMap.put(rs.getLong(PARENT.reference()), rs.getInt("count"));
            }
            return actionsMap;
        });
    }

    public Map<Long, ExceptionInfo> getSmtpRequestExceptions(Long[] ids) throws SQLException{
        return this.getSubRequestExceptions(EXCEPTION.column(PARENT).in(ids).and(EXCEPTION.column(TYPE).eq(SMTP.name())));
    }


    public List<Mail> getSmtpRequestMails(long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(SMTP_MAIL, SUBJECT, FROM, RECIPIENTS, MEDIA, REPLY_TO, SIZE, PARENT)
                )
                .filters(SMTP_MAIL.column(PARENT).eq(id));
        return v.build().execute(ds, rs -> {
            List<Mail> mails = new ArrayList<>();
            while (rs.next()) {
                var mail = new Mail();
                mail.setContentType(rs.getString(MEDIA.reference()));
                mail.setFrom(ofNullable(rs.getString(FROM.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setRecipients(ofNullable(rs.getString(RECIPIENTS.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setReplyTo(ofNullable(rs.getString(REPLY_TO.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                mail.setSize(rs.getInt(SIZE.reference()));
                mail.setSubject(rs.getString(SUBJECT.reference()));
                mails.add(mail);
            }
            return mails;
        });
    }

    public NamingRequest getLdapRequestsComplete(long id) throws SQLException {
        return requireSingle(getLdapRequestsComplete(LDAP_REQUEST.column(ID).eq(id)));
    }

    public List<NamingRequest> getLdapRequestsComplete(List<String> cdSession) throws SQLException {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<NamingRequest> getLdapRequestsComplete(String cdSession) throws SQLException {
        return getLdapRequestsComplete(LDAP_REQUEST.column(PARENT).eq(cdSession));
    }
    public List<NamingRequest> getLdapRequestsLazy(String cdSession) throws SQLException {
        return getLdapRequestsLazy(LDAP_REQUEST.column(PARENT).eq(cdSession));
    }

    public List<DtoRequest> getLdapRequestsLazyForSearch(JqueryRequestFilter jsf) throws SQLException{
        List<DtoRequest> mergeList= new ArrayList<>();
        mergeList.addAll(getLdapRequestsByFilter(jsf.filters(LDAP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{LDAP_REQUEST.join(EXCEPTION_JOIN).build(),LDAP_REQUEST.join(REST_SESSION_JOIN).build(),REST_SESSION.join(INSTANCE_JOIN).build()},
                "rest",
                new ColumnProxy[]{DBColumn.constant(null).as("type")}));
        mergeList.addAll(getLdapRequestsByFilter(jsf.filters(LDAP_REQUEST).toArray(DBFilter[]::new),
                new ViewJoin[][]{LDAP_REQUEST.join(EXCEPTION_JOIN).build(),LDAP_REQUEST.join(MAIN_SESSION_JOIN).build(),MAIN_SESSION.join(INSTANCE_JOIN).build()},
          "main",
                getColumns(MAIN_SESSION,TYPE)));

        return  mergeList;
    }

    public List<DtoRequest> getLdapRequestsByFilter(DBFilter[] filters, ViewJoin[][] joins, String type, NamedColumn[] mainType) throws SQLException {
        var count = getRequestCountByTable(LDAP_REQUEST,filters,joins);
        if(count > requestLimit){
            throw new PayloadTooLargeException();
        }

        var v = new QueryBuilder()
                .columns(mainType)
                .columns(DBColumn.constant(type).as("sessiontype"))
                .columns(
                        getColumns(
                                LDAP_REQUEST, ID, HOST, START, END, THREAD, STATUS, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(Stream.of(joins).flatMap(Arrays::stream).toArray(ViewJoin[]::new))
                .filters(filters)
                .orders(LDAP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DtoRequest> outs = new ArrayList<>();
            while (rs.next()) {
                DtoRequest out = new DtoRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                out.setId(rs.getString(PARENT.reference()));
                out.setType(rs.getString(TYPE.reference()));
                out.setSessionType(rs.getString("sessiontype"));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<NamingRequest> getLdapRequestsComplete(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            LDAP_REQUEST, ID, HOST, PORT, PROTOCOL, START, END, USER, THREAD, STATUS, PARENT
                    ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<NamingRequest> outs = new ArrayList<>();
            while (rs.next()) {
                NamingRequest out = new NamingRequest();
                out.setCdSession(rs.getString(PARENT.reference()));
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setProtocol(rs.getString(PROTOCOL.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<NamingRequest> getLdapRequestsLazy(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                LDAP_REQUEST, ID, HOST, START, END, THREAD, STATUS
                        ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<NamingRequest> outs = new ArrayList<>();
            while (rs.next()) {
                NamingRequest out = new NamingRequest();
                out.setIdRequest(rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setActions(new ArrayList<>());
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<NamingRequestStage> getLdapRequestStages(Long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            LDAP_STAGE, NAME, START, END, ARG, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LDAP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(LDAP_STAGE.column(PARENT).eq(id))
                .orders(LDAP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<NamingRequestStage> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new NamingRequestStage();
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                actions.add(action);
            }
            return actions;
        });
    }

    public Map<Long, List<String>> getLdapRequestStages(Long[] ids ) throws SQLException{
        return getLdapRequestStages(LDAP_STAGE.column(PARENT).in(ids));
    }

    public Map<Long, List<String>> getLdapRequestStages(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                LDAP_STAGE, NAME, PARENT
                        ))
                .filters(filter.and(LDAP_STAGE.column(NAME).notIn("CONNECTION","DISCONNECTION")));
        return v.build().execute(ds, rs -> {
            Map<Long,List<String>> actionsMap= new HashMap<>();
            while (rs.next()) {
                if(!actionsMap.containsKey(rs.getLong(PARENT.reference()))){
                    actionsMap.put(rs.getLong(PARENT.reference()), new ArrayList<>());
                }
                actionsMap.get(rs.getLong(PARENT.reference())).add(rs.getString(NAME.reference()));
            }
            return actionsMap;
        });
    }

    public Map<Long, ExceptionInfo> getLdapRequestExceptions(Long[] ids) throws SQLException{
        return this.getSubRequestExceptions(EXCEPTION.column(PARENT).in(ids).and(EXCEPTION.column(TYPE).eq(LDAP.name())));
    }


    public List<String> getRequestsHostsByType(String  type, String environement, Instant start, Instant end) throws SQLException{
        var Table = TraceApiTable.valueOf(RequestType.valueOf(type).getTable());
        var v1 = new QueryBuilder()
                .columns(getColumns(Table,HOST))
                .filters(Table.column(START).ge(from(start)))
                .filters(Table.column(START).lt(from(end)))
                .filters(Table.column(PARENT)
                .in(SessionIdByType_Environement_period("rest_session",environement,start,end))).distinct();

       var v2 = new QueryBuilder()
                .columns(getColumns(Table,HOST))
                .filters(Table.column(START).ge(from(start)))
                .filters(Table.column(START).lt(from(end)))
                .filters(Table.column(PARENT)
                .in(SessionIdByType_Environement_period("main_session",environement,start,end))).distinct();

       var r = v1.toString() + " UNION " + v2.toString();

        Object[] args = new Object[]{from(start), from(end), environement, from(start), from(end), "SERVER", from(start), from(end), environement, from(start), from(end)};
        int[] argTypes = new int[]{TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, TIMESTAMP, TIMESTAMP};
        return template.query(r, args, argTypes, rs -> {
            List<String> hosts = new ArrayList<>();
            while (rs.next()) {
                hosts.add(rs.getString(HOST.reference()));
            }
            return hosts;
        });
    }

    public SingleColumnQuery SessionIdByType_Environement_period(String type, String environement, Instant start, Instant end){
        var Table = TraceApiTable.valueOf(RequestType.valueOf(type).getTable());
        var v  = new QueryBuilder()
                .columns(getColumns(Table,ID))
                .joins(Table.join(INSTANCE_JOIN).build())
                .filters(INSTANCE.column(ENVIRONEMENT).eq(environement))
                .filters(Table.column(START).ge(from(start)))
                .filters(Table.column(START).lt(from(end)));
        if(Objects.equals(type, "rest_session")){
            v.filters(INSTANCE.column(TYPE).eq("SERVER"));
        }

        return v.asView().asColumn();
    }

    private String getPropertyByFilters(TraceApiTable table, TraceApiColumn target, DBFilter filters) throws SQLException { // main / apissesion
        var v = new QueryBuilder().columns(getColumns(table,target)).filters(filters);
        return v.build().execute(ds, rs -> {
            if(rs.next()){
                return rs.getString(target.reference()); // to be changed
            }
            return null;
        });
    }

    private static NamedColumn[] getColumns(ViewDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(NamedColumn[]::new);
    }

    public static ExceptionInfo getExceptionInfoIfNotNull(String className, String message) {
        if(className != null || message != null) {
            return new ExceptionInfo(className, message);
        }
        return null;
    }

    private static <T extends Enum<T>> T valueOfNullable(Class<T> classe, String value) {
        return ofNullable(value)
                .flatMap(v -> Stream.of(classe.getEnumConstants()).filter(e -> e.name().equals(v)).findAny())
                .orElse(null);
    }

    private static <T extends Enum<T>> List<T> valueOfNullabletoEnumList(Class<T> classe, String values){
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
