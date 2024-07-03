package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.model.Exchange;
import org.usf.inspect.server.model.InstanceMainSession;
import org.usf.inspect.server.model.InstanceRestSession;
import org.usf.inspect.server.model.InstanceSession;
import org.usf.inspect.server.model.filter.JqueryMainSessionFilter;
import org.usf.inspect.server.model.filter.JqueryRequestSessionFilter;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final DataSource ds;
    private final RequestDao dao;

    public void addInstanceEnvironment(List<InstanceEnvironmentWrapper> instances) {
        dao.saveInstanceEnvironment(instances);
    }

    public void addSessions(List<InstanceSession> sessions) {
        dao.saveSessions(sessions);
    }

    public Session getMainTreeById(String id) {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getRestSessionsForTree(prntIds);
        Session session = getMainSessionForTree(id);
        if(session != null){
            prntIncList.add(session);
        }
        createTree(prntIncList);
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    public Session getRestTreeById(String id) {
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

    public Map<String,String> getSessionParent(String childId){

        var prnt = getPropertyByFilters(REST_REQUEST, PARENT , REST_REQUEST.column(ID).equal(childId));
        if(prnt != null){
            var res = getPropertyByFilters(REST_SESSION, ID, REST_SESSION.column(ID).equal(prnt));
            if(res != null) {
                return Map.of("id", res, "type", "api");
            }
            res = getPropertyByFilters(MAIN_SESSION, ID, MAIN_SESSION.column(ID).equal(prnt));
            if(res!= null){
                return Map.of("id", res, "type", "main");
            }
        }
        return Collections.emptyMap();
    }

    public InstanceEnvironmentWrapper getInstanceById(String id) {
        var v = new RequestQueryBuilder();
        v.select(INSTANCE.table(),
                getColumns(INSTANCE, ID, TYPE, START, APP_NAME, VERSION, ADDRESS,
                ENVIRONEMENT, OS, RE, COLLECTOR)
        ).filters(
                INSTANCE.column(ID).equal(id)
        );
        return v.build().execute(ds, rs -> {
            if(rs.next()) {
                return new InstanceEnvironmentWrapper(
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

    public Session getRestSession(String id){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getRestSessions(jsf));
    }

    public List<Session> getRestSessionsForTree(List<String> ids){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getRestSessions(jsf);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().stream().toList();
            getRestRequests(parentIds, Exchange::new).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(parentIds).forEach(q -> reqMap.get(q.getParentId()).append(q.getDatabaseRequest()));
        }
        return sessions;
    }

    public List<Session> getRestSessions(JqueryRequestSessionFilter jsf) {
        var v = new RequestQueryBuilder();
        v.tables(REST_SESSION.table(), INSTANCE.table()).columns(
                getColumns(
                        REST_SESSION, ID, API_NAME, METHOD,
                    PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT,
                    START, END, THREAD, ERR_TYPE, ERR_MSG, USER_AGT, MASK
                )
        ).columns(getColumns(INSTANCE, APP_NAME, USER)).filters(REST_SESSION.column(INSTANCE_ENV).equal(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(REST_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                InstanceRestSession session = new InstanceRestSession();
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
                session.setInstanceUser(rs.getString(USER.reference()));
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setCacheControl(rs.getString(CACHE_CONTROL.reference()));
                session.setMask(rs.getInt(MASK.reference()));
                sessions.add(session);
            }
            return sessions;
        });
    }

    public Session getMainSessionForTree(String id){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        Session session = requireSingle(getMainSessions(jsf));
        if (session != null) {
            getRestRequests(session.getId(), Exchange::new).forEach(r -> session.append(r.getRequest()));
            getRunnableStages(session.getId()).forEach(r -> session.append(r.getStage()));
            getDatabaseRequests(session.getId()).forEach(d -> session.append(d.getDatabaseRequest()));
        }
        return session;
    }

    public Session getMainSession(String id){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return requireSingle(getMainSessions(jsf));
    }

    public List<Session> getMainSessions(JqueryMainSessionFilter jsf) {
        var v = new RequestQueryBuilder();
        v.tables(MAIN_SESSION.table(), INSTANCE.table()).columns(
                getColumns(
                        MAIN_SESSION, ID, NAME, START, END, TYPE, LOCATION, THREAD,
                        ERR_TYPE, ERR_MSG, MASK
                )
        ).columns(getColumns(INSTANCE, APP_NAME, USER)).filters(MAIN_SESSION.column(INSTANCE_ENV).equal(INSTANCE.column(ID)));;
        if(jsf != null) {
            v.filters(jsf.filters(MAIN_SESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                InstanceMainSession main = new InstanceMainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                main.setAppName(rs.getString(APP_NAME.reference()));
                main.setInstanceUser(rs.getString(USER.reference()));
                main.setMask(rs.getInt(MASK.reference()));
                sessions.add(main);
            }
            return sessions;
        });
    }
    public List<RestRequestWrapper> getRestRequests(String cdSession, Supplier<? extends RestRequest> fn) {
        return getRestRequests(Collections.singletonList(cdSession), fn);
    }

    private List<RestRequestWrapper> getRestRequests(List<String> cdSessions, Supplier<? extends RestRequest> fn) { //use criteria
        var v = new RequestQueryBuilder();
        v.tables(REST_REQUEST.table(), EXCEPTION.table())
                .columns(getColumns(
                        REST_REQUEST, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, REMOTE, PARENT
                ))
                .columns(getColumns(EXCEPTION, ERR_TYPE, ERR_MSG))
                .filters(REST_REQUEST.column(ID).equal(EXCEPTION.column(PARENT)))
                .filters(REST_REQUEST.column(PARENT).in(cdSessions.toArray()))
                .orders(REST_REQUEST.column(START).order());
        List<RestRequestWrapper> requests = v.build().execute(ds, rs -> {
            List<RestRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestWrapper out = new RestRequestWrapper(rs.getString(PARENT.reference()), fn);
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
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
        if(!requests.isEmpty()) {

        }
    }

    public List<LocalRequestWrapper> getRunnableStages(String cdSession){
        return getRunnableStages(Collections.singletonList(cdSession));
    }

    private List<LocalRequestWrapper> getRunnableStages(List<String> cdSessions){
        var v = new RequestQueryBuilder();
        v.select(
                LOCAL_REQUEST.table(),
                getColumns(
                        LOCAL_REQUEST, NAME, LOCATION, START, END, USER, THREAD, ERR_TYPE, ERR_MSG, PARENT
                )
        ).filters(LOCAL_REQUEST.column(PARENT).in(cdSessions.toArray())).orders(LOCAL_REQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<LocalRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                LocalRequestWrapper out = new LocalRequestWrapper(rs.getString(PARENT.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }
    public DatabaseRequestWrapper getDatabaseRequest(long idDatabase) {
        return requireSingle(getDatabaseRequests(DATABASE_REQUEST.column(ID).equal(idDatabase)));
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(List<String> cdSession) {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).in(cdSession.toArray()));
    }
    public List<DatabaseRequestWrapper> getDatabaseRequests(String cdSession) {
        return getDatabaseRequests(DATABASE_REQUEST.column(PARENT).in(cdSession));
    }

    private List<DatabaseRequestWrapper> getDatabaseRequests(DBFilter filter) {
        var v = new RequestQueryBuilder();
        v.select(
                DATABASE_REQUEST.table(),
                getColumns(
                        DATABASE_REQUEST, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                        DB_NAME, DB_VERSION, COMMANDS, COMPLETE, PARENT
                )
        ).filters(filter).orders(DATABASE_REQUEST.column(START).order());

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
                out.setCommands(valueOfNullabletoEnumList(org.usf.inspect.jdbc.SqlCommand.class, rs.getString(COMMANDS.reference())));
                out.setActions(new ArrayList<>());
                out.setCompleted("T".equals(rs.getString(COMPLETE.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<DatabaseRequestStage> getDatabaseActions(Long id) {
        var v = new RequestQueryBuilder();
        v.select(
                DATABASE_STAGE.table(),
                getColumns(DATABASE_STAGE, NAME, START, END, ERR_TYPE, ERR_MSG, ACTION_COUNT, PARENT)
        ).filters(DATABASE_STAGE.column(PARENT).equal(id)).orders(DATABASE_STAGE.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseRequestStage> actions = new ArrayList<>();
            while (rs.next()) {
                var action = new DatabaseRequestStage();
                action.setName(rs.getString(TYPE.reference()));
                action.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                action.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                action.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                action.setCount(ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null));
                actions.add(action);
            }
            return actions;
        });
    }

    private List<ExceptionWrapper> getExceptions(List<Long> cdRequests) {
        var v = new RequestQueryBuilder();
        v.tables(EXCEPTION.table())
        v.select(EXCEPTION.table(), getColumns(EXCEPTION, ERR_TYPE, ERR_MSG, ORDER, PARENT))
                .filters(EXCEPTION.column(PARENT).in(cdRequests.toArray()));
        return v.build().execute(ds, rs -> {
            List<ExceptionWrapper> exceptions = new ArrayList<>();
            while (rs.next()) {
                exceptions.add(
                        new ExceptionWrapper(
                                rs.getLong(PARENT.reference()),
                                new ExceptionInfo(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())),
                                rs.getLong(ORDER.reference())
                        )
                );
            }
            return exceptions;
        });
    }

    public String getPropertyByFilters(TraceApiTable table, TraceApiColumn target, DBFilter filters){ // main / apissesion
        var v = new RequestQueryBuilder().select(table.table(), getColumns(table,target)).filters(filters);
        return v.build().execute(ds, rs -> {
            if(rs.next()){
                return rs.getString(target.reference()); // to be changed
            }
            return null;
        });
    }

    private static TaggableColumn[] getColumns(TableDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(TaggableColumn[]::new);
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
