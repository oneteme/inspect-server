package org.usf.inspect.server.service;

import static java.sql.Timestamp.from;
import static java.sql.Types.*;
import static java.sql.Types.BIGINT;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.DATABASE_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.DATABASE_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.EXCEPTION;
import static org.usf.inspect.server.config.TraceApiTable.FTP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.FTP_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.INSTANCE;
import static org.usf.inspect.server.config.TraceApiTable.LDAP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.LDAP_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.LOCAL_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.MAIN_SESSION;
import static org.usf.inspect.server.config.TraceApiTable.REST_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.REST_SESSION;
import static org.usf.inspect.server.config.TraceApiTable.SMTP_MAIL;
import static org.usf.inspect.server.config.TraceApiTable.SMTP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.SMTP_STAGE;
import static org.usf.inspect.server.config.constant.JoinConstant.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.FtpRequest;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.Mail;
import org.usf.inspect.core.MailRequest;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.NamingRequest;
import org.usf.inspect.core.NamingRequestStage;
import org.usf.inspect.core.RestRequest;
import org.usf.inspect.core.RestSession;
import org.usf.inspect.core.Session;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.jdbc.SqlCommand;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.NamedColumn;
import org.usf.jquery.core.QueryBuilder;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewDecorator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final JdbcTemplate template;
    private final DataSource ds;
    private final RequestDao dao;

    public void addInstance(ServerInstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    @TraceableStage
    @Transactional(rollbackFor = Throwable.class)
    public long addSessions(List<ServerSession> sessions) {
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
                        Optional<RestRequest> opt = prntB.getRestRequests().stream()
                                .filter(k -> prntA.getId().equals(k.getId()))
                                .findFirst();
                        if (opt.isPresent()) {
                            var ex = (Exchange) opt.get();
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
        return template.query(v5, args, argTypes, (ResultSet rs) -> {
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
        /*return v.build().execute(ds, rs -> {
            Map<String, List<Architecture>> map = new HashMap<>();
            while(rs.next()) {
                var key = rs.getString(APP_NAME.reference());
                if(!map.containsKey(key)) {
                    map.put(key, new ArrayList<>());
                }
                map.get(key).add(new Architecture(rs.getString(DB.reference()), rs.getString(SCHEMA.reference()), rs.getString("type"), null));
            }
            return map.entrySet().stream().map(entry -> new Architecture(entry.getKey(), null, null, entry.getValue())).toList();
        });*/
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
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

    public ServerInstanceEnvironment getInstance(String id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                INSTANCE, ID, USER, TYPE, START, APP_NAME, VERSION, ADDRESS,
                                ENVIRONEMENT, OS, RE, COLLECTOR
                        ))
                .filters(INSTANCE.column(ID).eq(id));
        return v.build().execute(ds, rs -> {
            if(rs.next()) {
                return new ServerInstanceEnvironment(
                        rs.getString(ID.reference()),
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference()),
                        rs.getString(USER.reference()),
                        InstanceType.valueOf(rs.getString(TYPE.reference())),
                        fromNullableTimestamp(rs.getTimestamp(START.reference())),
                        rs.getString(COLLECTOR.reference()));
            }
            return null;
        });
    }

    public Session getRestSession(String id) throws SQLException{
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getRestSessions(jsf));
    }

    public List<Session> getRestSessionsForTree(List<String> ids) throws SQLException {
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getRestSessions(jsf);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().stream().toList();
            getRestRequests(parentIds, Exchange::new).forEach(r -> reqMap.get(r.getCdSession()).append(r.getRequest()));
            getLocalRequests(parentIds).forEach(r -> reqMap.get(r.getCdSession()).append(r.getStage()));
            getDatabaseRequests(parentIds).forEach(q -> reqMap.get(q.getCdSession()).append(q.getDatabaseRequest()));
            getFtpRequests(parentIds).forEach(q -> reqMap.get(q.getCdSession()).append(q.getFtpRequest()));
            getSmtpRequests(parentIds).forEach(q -> reqMap.get(q.getCdSession()).append(q.getSmtpRequest()));
            getLdapRequests(parentIds).forEach(q -> reqMap.get(q.getCdSession()).append(q.getLdapRequest()));
        }
        return sessions;
    }

    @Deprecated
    public List<Session> getRestSessionsForSearch(JqueryRequestSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                REST_SESSION, ID, API_NAME, METHOD,
                                PROTOCOL, PORT, PATH, QUERY, STATUS, SIZE_IN, SIZE_OUT,
                                START, END, USER
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                ServerRestSession session = new ServerRestSession();
                session.setId(rs.getString(ID.reference()));
                session.setMethod(rs.getString(METHOD.reference()));
                session.setProtocol(rs.getString(PROTOCOL.reference()));
                session.setPort(rs.getInt(PORT.reference()));
                session.setPath(rs.getString(PATH.reference()));
                session.setQuery(rs.getString(QUERY.reference()));
                session.setStatus(rs.getInt(STATUS.reference()));
                session.setInDataSize(rs.getLong(SIZE_IN.reference()));
                session.setOutDataSize(rs.getLong(SIZE_OUT.reference()));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setRestRequests(new ArrayList<>());
                session.setLocalRequests(new ArrayList<>());
                session.setDatabaseRequests(new ArrayList<>());
                sessions.add(session);
            }
            return sessions;
        });
    }

    public List<Session> getRestSessions(JqueryRequestSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            REST_SESSION, ID, API_NAME, METHOD,
                            PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT,
                            START, END, THREAD, ERR_TYPE, ERR_MSG, MASK, USER, USER_AGT, CACHE_CONTROL, INSTANCE_ENV
                    ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            ColumnDecorator[] columns = {USER_AGT};
            while (rs.next()) {
                ServerRestSession session = new ServerRestSession();
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
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                session.setMask(rs.getInt(MASK.reference()));
                session.setRestRequests(new ArrayList<>());
                session.setLocalRequests(new ArrayList<>());
                session.setDatabaseRequests(new ArrayList<>());
                session.setFtpRequests(new ArrayList<>());
                session.setMailRequests(new ArrayList<>());
                session.setLdapRequests(new ArrayList<>());
                sessions.add(session);
            }
            return sessions;
        });
    }

    public Session getMainSessionForTree(String id) throws SQLException {
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        Session session = requireSingle(getMainSessions(jsf));
        if (session != null) {
            getRestRequests(session.getId(), Exchange::new).forEach(r -> session.append(r.getRequest()));
            getLocalRequests(session.getId()).forEach(r -> session.append(r.getStage()));
            getDatabaseRequests(session.getId()).forEach(d -> session.append(d.getDatabaseRequest()));
            getFtpRequests(session.getId()).forEach(q -> session.append(q.getFtpRequest()));
            getSmtpRequests(session.getId()).forEach(q -> session.append(q.getSmtpRequest()));
            getLdapRequests(session.getId()).forEach(q -> session.append(q.getLdapRequest()));
        }
        return session;
    }

    public Session getMainSession(String id) throws SQLException{
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getMainSessions(jsf));
    }

    @Deprecated
    public List<Session> getMainSessionsForSearch(JqueryMainSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                MAIN_SESSION, ID, NAME, START, END, LOCATION, TYPE,
                                USER
                        ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));;
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                ServerMainSession main = new ServerMainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setAppName(rs.getString(APP_NAME.reference()));
                main.setUser(rs.getString(USER.reference()));
                main.setType(rs.getString(TYPE.reference()));
                main.setRestRequests(new ArrayList<>());
                main.setLocalRequests(new ArrayList<>());
                main.setDatabaseRequests(new ArrayList<>());
                sessions.add(main);
            }
            return sessions;
        });
    }

    public List<Session> getMainSessions(JqueryMainSessionFilter jsf) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD,
                            ERR_TYPE, ERR_MSG, MASK, USER, INSTANCE_ENV
                    ))
                .columns(getColumns(INSTANCE, APP_NAME))
                .filters(MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)));;
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                ServerMainSession main = new ServerMainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                main.setAppName(rs.getString(APP_NAME.reference()));
                main.setUser(rs.getString(USER.reference()));
                main.setInstanceId(rs.getString(INSTANCE_ENV.reference()));
                main.setRestRequests(new ArrayList<>());
                main.setLocalRequests(new ArrayList<>());
                main.setDatabaseRequests(new ArrayList<>());
                main.setFtpRequests(new ArrayList<>());
                main.setMailRequests(new ArrayList<>());
                main.setLdapRequests(new ArrayList<>());
                main.setMask(rs.getInt(MASK.reference()));
                sessions.add(main);
            }
            return sessions;
        });
    }

    public List<RestRequestWrapper> getRestRequests(String cdSession, Supplier<? extends RestRequest> fn) throws SQLException {
        return getRestRequests(Collections.singletonList(cdSession), fn);
    }

    private List<RestRequestWrapper> getRestRequests(List<String> cdSessions, Supplier<? extends RestRequest> fn) throws SQLException { //use criteria
        var v = new QueryBuilder()
                .columns(getColumns(
                        REST_REQUEST, ID, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, REMOTE, PARENT
                ))
                //.columns(REST_REQUEST.column(PARENT).as("test"), EXCEPTION.column(PARENT).as("test2"))
                .filters(REST_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(REST_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<RestRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestWrapper out = new RestRequestWrapper(rs.getString(PARENT.reference()), fn);
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
                outs.add(out);
            }
            return outs;
        });
    }

    public List<LocalRequestWrapper> getLocalRequests(String cdSession) throws SQLException{
        return getLocalRequests(Collections.singletonList(cdSession));
    }

    private List<LocalRequestWrapper> getLocalRequests(List<String> cdSessions) throws SQLException{
        var v = new QueryBuilder()
                .columns(getColumns(
                        LOCAL_REQUEST, ID, NAME, LOCATION, START, END, USER, THREAD, STATUS, PARENT
                ))
                .filters(LOCAL_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(LOCAL_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<LocalRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                LocalRequestWrapper out = new LocalRequestWrapper(rs.getString(PARENT.reference()));
                out.setId(rs.getLong(ID.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }


    public DatabaseRequestWrapper getDatabaseRequest(long idDatabase) throws SQLException {
        return requireSingle(getDatabaseRequests(DATABASE_REQUEST.column(ID).eq(idDatabase)));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(List<String> cdSession) throws SQLException {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(String cdSession) throws SQLException {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<DatabaseRequestWrapper> getDatabaseRequests(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            DATABASE_REQUEST, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                            DB_NAME, DB_VERSION, COMMANDS, STATUS, PARENT
                    ))
                .filters(filter)
                .orders(DATABASE_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper(rs.getString(PARENT.reference()));
                out.setId(rs.getLong(ID.reference()));
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
                out.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(COMMANDS.reference())));
                out.setStatus(rs.getBoolean(STATUS.reference()));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<DatabaseRequestStageWrapper> getDatabaseRequestStages(Long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                        getColumns(
                                DATABASE_STAGE, NAME, START, END, ACTION_COUNT, ORDER, PARENT
                        ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(DATABASE_STAGE.join(EXCEPTION_JOIN).build())
                .filters(DATABASE_STAGE.column(PARENT).eq(id))
                .orders(DATABASE_STAGE.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new DatabaseRequestStageWrapper(new DatabaseRequestStage());
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                action.setOrder(rs.getInt(ORDER.reference()));
                actions.add(action);
            }
            return actions;
        });
    }

    public FtpRequestWrapper getFtpRequest(long id) throws SQLException {
        return requireSingle(getFtpRequests(FTP_REQUEST.column(ID).eq(id)));
    }

    public List<FtpRequestWrapper> getFtpRequests(List<String> cdSession) throws SQLException {
        return getFtpRequests(FTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<FtpRequestWrapper> getFtpRequests(String cdSession) throws SQLException {
        return getFtpRequests(FTP_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<FtpRequestWrapper> getFtpRequests(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            FTP_REQUEST, ID, HOST, PORT, PROTOCOL, SERVER_VERSION, CLIENT_VERSION, START, END, USER, THREAD, STATUS, PARENT
                    )
                )
                .filters(filter)
                .orders(FTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequestWrapper out = new FtpRequestWrapper(rs.getString(PARENT.reference()), new FtpRequest());
                out.setId(rs.getLong(ID.reference()));
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

    public List<FtpRequestStageWrapper> getFtpRequestStages(long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            FTP_STAGE, NAME, START, END, ARG, ORDER, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(FTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(FTP_STAGE.column(PARENT).eq(id))
                .orders(FTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<FtpRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new FtpRequestStageWrapper(new FtpRequestStage());
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setArgs(ofNullable(rs.getString(ARG.reference())).map(str -> Arrays.stream(str.split(",")).toArray(String[]::new)).orElse(null));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                action.setOrder(rs.getInt(ORDER.reference()));
                actions.add(action);
            }
            return actions;
        });
    }

    public MailRequestWrapper getSmtpRequest(long id) throws SQLException {
        return requireSingle(getSmtpRequests(SMTP_REQUEST.column(ID).eq(id)));
    }

    public List<MailRequestWrapper> getSmtpRequests(List<String> cdSession) throws SQLException {
        return getSmtpRequests(SMTP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<MailRequestWrapper> getSmtpRequests(String cdSession) throws SQLException {
        return getSmtpRequests(SMTP_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<MailRequestWrapper> getSmtpRequests(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            SMTP_REQUEST, ID, HOST, PORT, START, END, USER, THREAD, STATUS, PARENT
                    ))
                .filters(filter)
                .orders(SMTP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<MailRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequestWrapper out = new MailRequestWrapper(rs.getString(PARENT.reference()), new MailRequest());
                out.setId(rs.getLong(ID.reference()));
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

    public List<MailRequestStageWrapper> getSmtpRequestStages(long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(SMTP_STAGE, NAME, START, END, ORDER, PARENT)
                )
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(SMTP_STAGE.join(EXCEPTION_JOIN).build())
                .filters(SMTP_STAGE.column(PARENT).eq(id))
                .orders(SMTP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<MailRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new MailRequestStageWrapper(new MailRequestStage());
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                action.setOrder(rs.getInt(ORDER.reference()));
                actions.add(action);
            }
            return actions;
        });
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

    public NamingRequestWrapper getLdapRequest(long id) throws SQLException {
        return requireSingle(getLdapRequests(LDAP_REQUEST.column(ID).eq(id)));
    }

    public List<NamingRequestWrapper> getLdapRequests(List<String> cdSession) throws SQLException {
        return getLdapRequests(LDAP_REQUEST.column(PARENT).in(cdSession.toArray()));
    }

    public List<NamingRequestWrapper> getLdapRequests(String cdSession) throws SQLException {
        return getLdapRequests(LDAP_REQUEST.column(PARENT).eq(cdSession));
    }

    private List<NamingRequestWrapper> getLdapRequests(DBFilter filter) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            LDAP_REQUEST, ID, HOST, PORT, PROTOCOL, START, END, USER, THREAD, STATUS, PARENT
                    ))
                .filters(filter)
                .orders(LDAP_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<NamingRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                NamingRequestWrapper out = new NamingRequestWrapper(rs.getString(PARENT.reference()), new NamingRequest());
                out.setId(rs.getLong(ID.reference()));
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

    public List<NamingRequestStageWrapper> getLdapRequestStages(Long id) throws SQLException {
        var v = new QueryBuilder()
                .columns(
                    getColumns(
                            LDAP_STAGE, NAME, START, END, ARG, ORDER, PARENT
                    ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .joins(LDAP_REQUEST.join(EXCEPTION_JOIN).build())
                .filters(LDAP_STAGE.column(PARENT).eq(id))
                .orders(LDAP_STAGE.column(ORDER).order());
        return v.build().execute(ds, rs -> {
            List<NamingRequestStageWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new NamingRequestStageWrapper(new NamingRequestStage());
                action.setName(rs.getString(NAME.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                action.setOrder(rs.getInt(ORDER.reference()));
                actions.add(action);
            }
            return actions;
        });
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

    private static Instant fromNullableTimestamp(Timestamp timestamp) {
        return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
    }

    private static ExceptionInfo getExceptionInfoIfNotNull(String className, String message) {
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
