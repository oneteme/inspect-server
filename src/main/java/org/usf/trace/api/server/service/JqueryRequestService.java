package org.usf.trace.api.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.usf.jquery.core.DBFilter;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;
import org.usf.trace.api.server.config.TraceApiColumn;
import org.usf.trace.api.server.config.TraceApiTable;
import org.usf.trace.api.server.model.Exchange;
import org.usf.trace.api.server.dao.RequestDao;
import org.usf.trace.api.server.model.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.model.filter.JqueryRequestSessionFilter;
import org.usf.trace.api.server.model.wrapper.DatabaseActionWrapper;
import org.usf.trace.api.server.model.wrapper.DatabaseRequestWrapper;
import org.usf.trace.api.server.model.wrapper.ApiRequestWrapper;
import org.usf.trace.api.server.model.wrapper.RunnableStageWrapper;
import org.usf.traceapi.core.*;
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
public class JqueryRequestService {

    private final DataSource ds;
    private final RequestDao dao;

    public void addSessions(List<Session> sessions) {
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
                    Optional<ApiRequest> opt = prntB.getRequests().stream()
                            .filter(k -> prntA.getId().equals(k.getId()))
                            .findFirst();
                    if (opt.isPresent()) {
                        var ex = (Exchange) opt.get();
                        ex.setRemoteTrace((ApiSession) prntA);
                    }
                }
            }));
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }
    public Map<String,String> getSessionParent (String childId){

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

    public String getPropertyByFilters(TraceApiTable table, TraceApiColumn target, DBFilter filters){ // main / apissesion
        var v = new RequestQueryBuilder().select(table.table(), getColumns(table,target)).filters(filters);
        return v.build().execute(ds, rs -> {
            if(rs.next()){
                return rs.getString(target.reference()); // to be changed
            }
            return null;
        });
    }

    public List<Session> getApiSessionById(List<String> ids, Supplier<? extends ApiRequest> fn, boolean queryLazy){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new), true);
        return getApiSesssionsByCriteria(jsf, fn, queryLazy);
    }

    public List<Session> getApiSesssionsByCriteria(JqueryRequestSessionFilter jsf, Supplier<? extends ApiRequest> fn, boolean queryLazy) {
        var v = new RequestQueryBuilder();
        v.select(
                APISESSION.table(),
                getColumns(
                        APISESSION, ID, API_NAME, APP_NAME, METHOD,
                        PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT,
                        START, END, THREAD, ERR_TYPE, ERR_MSG, USER, VERSION, ADDRESS, ENVIRONEMENT,
                        OS, RE
                )
        );
        if(jsf != null) {
            v.filters(jsf.filters(APISESSION).toArray(DBFilter[]::new));
        }
        List<Session> res = v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                ApiSession session = new ApiSession();
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
                session.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                session.setThreadName(rs.getString(THREAD.reference()));
                session.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                session.setName(rs.getString(API_NAME.reference()));
                session.setUser(rs.getString(USER.reference()));
                session.setApplication(new ApplicationInfo(
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference())
                ));
                sessions.add(session);
            }
            return sessions;
        });
        if (Objects.requireNonNull(jsf).isLazy() && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().toArray(String[]::new);
            getApiRequests(parentIds, fn).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds),queryLazy).forEach(q -> reqMap.get(q.getParentId()).append(q));
        }
        return res;
    }

    public List<Session> getMainSessionById(String id, Supplier<? extends ApiRequest> fn, boolean queryLazy){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new), true);
        return getMainSessionsByCriteria(jsf, fn, queryLazy);
    }

    public List<Session> getMainSessionsByCriteria(JqueryMainSessionFilter jsf, Supplier<? extends ApiRequest> fn,boolean queryLazy) {
        var v = new RequestQueryBuilder();
        v.select(
                MAINSESSION.table(),
                getColumns(
                        MAINSESSION, ID, NAME, START, END, USER, OS, RE, TYPE, LOCATION, THREAD,
                        APP_NAME, VERSION, ADDRESS, ENVIRONEMENT, ERR_TYPE, ERR_MSG
                )
        );
        if(jsf != null) {
            v.filters(jsf.filters(MAINSESSION).toArray(DBFilter[]::new));
        }
        List<Session> res = v.build().execute(ds, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                MainSession main = new MainSession();
                main.setId(rs.getString(ID.reference())); // add value of nullable
                main.setName(rs.getString(NAME.reference()));
                main.setUser(rs.getString(USER.reference()));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(START.reference())));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(END.reference())));
                main.setLaunchMode(valueOfNullable(LaunchMode.class, rs.getString(TYPE.reference())));
                main.setLocation(rs.getString(LOCATION.reference()));
                main.setThreadName(rs.getString(THREAD.reference()));
                main.setApplication(new ApplicationInfo(
                        rs.getString(APP_NAME.reference()),
                        rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()),
                        rs.getString(RE.reference())
                ));
                main.setException(getExceptionInfoIfNotNull(rs.getString(ERR_TYPE.reference()), rs.getString(ERR_MSG.reference())));
                sessions.add(main);
            }

            return sessions;
        });
        if (Objects.requireNonNull(jsf).isLazy() && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().toArray(String[]::new);
            getApiRequests(parentIds, fn).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds),queryLazy).forEach(q -> reqMap.get(q.getParentId()).append(q));
        }
        return res;
    }
    public List<ApiRequestWrapper> getApiRequests(String[] incomingId, Supplier<? extends ApiRequest> fn) { //use criteria
        var v = new RequestQueryBuilder();
        v.select(
                APIREQUEST.table(),
                getColumns(
                        APIREQUEST, ID, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, START, END, THREAD, ERR_TYPE, ERR_MSG, PARENT
                )
        );
        v.filters(APIREQUEST.column(PARENT).in(incomingId));
        v.orders(APIREQUEST.column(START).order());
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
        );
        v.filters(STAGES.column(PARENT).in(ids));
        v.orders(STAGES.column(START).order());
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
                        DB_NAME, DB_VERSION, NAME, COMMANDS, LOCATION, COMPLETE, PARENT
                )
        );
        v.filters(filter);
        v.orders(DBQUERY.column(START).order());
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
                out.setLocation(rs.getString(LOCATION.reference()));
                out.setName(rs.getString(NAME.reference()));
                out.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(COMMANDS.reference())));
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
        );
        v.filters(DBACTION.column(PARENT).in(ids));
        v.orders(DBACTION.column(START).order());
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
