package org.usf.trace.api.server.service.v3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;
import org.usf.trace.api.server.config.TraceApiColumn;
import org.usf.trace.api.server.config.TraceApiTable;
import org.usf.trace.api.server.dao.v3.V3RequestDao;
import org.usf.trace.api.server.model.Exchange;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.model.filter.JqueryRequestSessionFilter;
import org.usf.trace.api.server.model.wrapper.*;
import org.usf.traceapi.core.*;
import org.usf.traceapi.jdbc.JDBCAction;

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
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.*;

@Repository
@RequiredArgsConstructor
public class JqueryV3RequestService {

    private final DataSource ds;
    private final V3RequestDao dao;

    public void addInstanceEnvironment(List<InstanceEnvironmentWrapper> instances) {
        dao.saveInstanceEnvironment(instances);
    }

    public void addSessions(List<InstanceSession> sessions) {
        dao.saveSessions(sessions);
    }

    public Session getTreeById(String id) {
        List<String> prntIds = dao.selectChildsById(id);
        List<Session> prntIncList = getApiSessionById(prntIds, Exchange::new, false);
        List<Session> sessionList = getMainSessionById(id, Exchange::new, false);
        if(sessionList != null && !sessionList.isEmpty()){
            prntIncList.add(sessionList.get(0));
        }
        prntIncList.forEach(prntA ->
                prntIncList.forEach(prntB -> {
                    if (!Objects.equals(prntA.getId(), prntB.getId())){
                        Optional<RestRequest> opt = prntB.getRequests().stream()
                                .filter(k -> prntA.getId().equals(k.getId()))
                                .findFirst();
                        if (opt.isPresent()) {
                            var ex = (Exchange) opt.get();
                            ex.setRemoteTrace((RestSession) prntA);
                        }
                    }
                }));
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }

    public Map<String,String> getSessionParent(String childId){

        var prnt = getPropertyByFilters(APIREQUEST, PARENT , APIREQUEST.column(ID).equal(childId));
        if(prnt != null){
            var res = getPropertyByFilters(APISESSION, ID, APISESSION.column(ID).equal(prnt));
            if(res != null) {
                return Map.of("id", res, "type", "api");
            }
            res = getPropertyByFilters(MAINSESSION, ID, MAINSESSION.column(ID).equal(prnt));
            if(res!= null){
                return Map.of("id", res, "type", "main");
            }
        }
        return Collections.emptyMap();
    }

    public InstanceEnvironmentWrapper getInstanceBySessionId(String id, TableDecorator tableDecorator) {
        var v = new RequestQueryBuilder();
        v.tables(INSTANCE.table(), tableDecorator.table()).columns(
                getColumns(INSTANCE, ID, TYPE, START, APP_NAME, VERSION, ADDRESS,
                    ENVIRONEMENT, OS, RE, COLLECTOR)
        ).filters(
                INSTANCE.column(ID).equal(tableDecorator.column(INSTANCE_ENV)),
                tableDecorator.column(INSTANCE_ENV).equal(id)
        );;
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

    public List<Session> getApiSessionById(List<String> ids, Supplier<? extends RestRequest> fn, boolean queryLazy){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new));
        List<Session> sessions = getApiSesssionsByCriteria(jsf, fn, queryLazy);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().toArray(String[]::new);
            getApiRequests(parentIds, fn).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds), queryLazy).forEach(q -> reqMap.get(q.getParentId()).append(q));
        }
        return sessions;
    }

    public List<Session> getApiSesssionsByCriteria(JqueryRequestSessionFilter jsf, Supplier<? extends RestRequest> fn, boolean queryLazy) {
        var v = new RequestQueryBuilder();
        v.tables(APISESSION.table(), INSTANCE.table()).columns(
                getColumns(
                    APISESSION, ID, API_NAME, METHOD,
                    PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT,
                    START, END, THREAD, ERR_TYPE, ERR_MSG, USER, USER_AGT
                )
        ).columns(getColumns(INSTANCE, APP_NAME)).filters(APISESSION.column(INSTANCE_ENV).equal(INSTANCE.column(ID)));
        if(jsf != null) {
            v.filters(jsf.filters(APISESSION).toArray(DBFilter[]::new));
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
                session.setAppName(rs.getString(APP_NAME.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setUserAgent(rs.getString(USER_AGT.reference()));
                sessions.add(session);
            }
            return sessions;
        });
    }

    public List<Session> getMainSessionById(String id, Supplier<? extends RestRequest> fn, boolean queryLazy){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        List<Session> sessions = getMainSessionsByCriteria(jsf, fn, queryLazy);
        if (!sessions.isEmpty()) {
            var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().toArray(String[]::new);
            getApiRequests(parentIds, fn).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds),queryLazy).forEach(q -> reqMap.get(q.getParentId()).append(q));
        }
        return sessions;
    }

    public List<Session> getMainSessionsByCriteria(JqueryMainSessionFilter jsf, Supplier<? extends RestRequest> fn, boolean queryLazy) {
        var v = new RequestQueryBuilder();
        v.tables(MAINSESSION.table(), INSTANCE.table()).columns(
                getColumns(
                        MAINSESSION, ID, NAME, START, END, USER, TYPE, LOCATION, THREAD,
                        ERR_TYPE, ERR_MSG
                )
        ).columns(getColumns(INSTANCE, APP_NAME)).filters(MAINSESSION.column(INSTANCE_ENV).equal(INSTANCE.column(ID)));;
        if(jsf != null) {
            v.filters(jsf.filters(MAINSESSION).toArray(DBFilter[]::new));
        }
        return v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                InstanceMainSession main = new InstanceMainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setUser(rs.getString(USER.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setType(rs.getString(TYPE.reference()));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setAppName(rs.getString(APP_NAME.reference()));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                sessions.add(main);
            }
            return sessions;
        });
    }
    public List<ApiRequestWrapper> getApiRequests(String[] incomingId, Supplier<? extends RestRequest> fn) { //use criteria
        var v = new RequestQueryBuilder();
        v.select(
                APIREQUEST.table(),
                getColumns(
                        APIREQUEST, ID, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, CONTENT_ENCODING_IN, CONTENT_ENCODING_OUT, START, END, THREAD, ERR_TYPE, ERR_MSG, PARENT
                )
        ).filters(APIREQUEST.column(PARENT).in(incomingId)).orders(APIREQUEST.column(START).order());
        return v.build().execute(ds, rs -> {
            List<ApiRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                ApiRequestWrapper out = new ApiRequestWrapper(rs.getString(PARENT.reference()), fn);
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
                out.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<RunnableStageWrapper> getRunnableStages(String[] ids){
        var v = new RequestQueryBuilder();
        v.select(
                STAGES.table(),
                getColumns(
                        STAGES, NAME, LOCATION, START, END, USER, THREAD, ERR_TYPE, ERR_MSG, PARENT
                )
        ).filters(STAGES.column(PARENT).in(ids)).orders(STAGES.column(START).order());
        return v.build().execute(ds, rs -> {
            List<RunnableStageWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RunnableStageWrapper out = new RunnableStageWrapper(rs.getString(PARENT.reference()));
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

    public List<DatabaseRequestWrapper> getDatabaseRequests(DBFilter filter, boolean queryLazy) {
        var v = new RequestQueryBuilder();
        v.select(
                DBQUERY.table(),
                getColumns(
                        DBQUERY, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                        DB_NAME, DB_VERSION, COMMANDS, COMPLETE, PARENT
                )
        ).filters(filter).orders(DBQUERY.column(START).order());
        var queries = v.build().execute(ds, rs -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper(rs.getString(PARENT.reference()), rs.getLong(ID.reference()));
                out.setHost(rs.getString(HOST.reference()));
                out.setPort(rs.getInt(PORT.reference()));
                out.setDatabase(rs.getString(DB.reference()));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                out.setUser(rs.getString(USER.reference()));
                out.setThreadName(rs.getString(THREAD.reference()));
                out.setDriverVersion(rs.getString(DRIVER.reference()));
                out.setDatabaseName(rs.getString(DB_NAME.reference()));
                out.setDatabaseVersion(rs.getString(DB_VERSION.reference()));
                out.setCommands(valueOfNullabletoEnumList(org.usf.traceapi.jdbc.SqlCommand.class, rs.getString(COMMANDS.reference())));
                out.setActions(new ArrayList<>());
                out.setCompleted("T".equals(rs.getString(COMPLETE.reference())));
                outs.add(out);
            }
            return outs;
        });
        if (queryLazy && !queries.isEmpty()) {
            var qMap = queries.stream().collect(toMap(DatabaseRequestWrapper::getId, identity())); //unique
            getDatabaseActions(qMap.keySet().toArray(Long[]::new)).forEach(a -> qMap.get(a.getParentId()).getActions().add(a.getAction()));
        }
        return queries;
    }

    public List<DatabaseActionWrapper> getDatabaseActions(Long[] ids) {
        var v = new RequestQueryBuilder();
        v.select(
                DBACTION.table(),
                getColumns(DBACTION, TYPE, START, END, ERR_TYPE, ERR_MSG, ACTION_COUNT, PARENT)
        ).filters(DBACTION.column(PARENT).in(ids)).orders(DBACTION.column(START).order());
        return v.build().execute(ds, rs -> {
            List<DatabaseActionWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                actions.add(
                        new DatabaseActionWrapper(
                                rs.getLong(PARENT.reference()),
                                JDBCAction.valueOf(rs.getString(TYPE.reference())),
                                fromNullableTimestamp(rs.getTimestamp(START.reference())),
                                fromNullableTimestamp(rs.getTimestamp(END.reference())),
                                getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())),
                                ofNullable(rs.getString(ACTION_COUNT.reference())).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null)
                        )
                );
            }
            return actions;
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
