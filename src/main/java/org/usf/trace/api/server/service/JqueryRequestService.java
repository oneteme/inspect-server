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
import org.usf.trace.api.server.config.DataConstants;
import org.usf.trace.api.server.model.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.model.filter.JqueryRequestSessionFilter;
import org.usf.trace.api.server.model.filter.JquerySessionFilter;
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
        List<Session> prntIncList = getApiSessionById(prntIds, Exchange::new);
        List<Session> sessionList = getMainSessionById(id, Exchange::new);
        if(sessionList != null && !sessionList.isEmpty()){
            prntIncList.add(sessionList.get(0));
        }
        prntIncList.forEach((prntA) -> {
            prntIncList.forEach((prntB) -> {
                if (!Objects.equals(prntA.getId(), prntB.getId())){
                    Optional<ApiRequest> opt = prntB.getRequests().stream()
                            .filter(k -> prntA.getId().equals(k.getId()))
                            .findFirst();
                    if (opt.isPresent()) {
                        var ex = (Exchange) opt.get();
                        ex.setRemoteTrace((ApiSession) prntA);
                    }
                }
            });
        });
        return prntIncList.stream().filter(r ->  r.getId().equals(id)).findFirst().orElseThrow();
    }
    public Map<String,String> getSessionParent ( String childId){
       Map<String,String> result = null;
        var res = getSessionParentByChildId("APISESSION", childId);
        if(res != null) {
            result = Map.of("id",res,"type","api");
        }
        else{
            res = getSessionParentByChildId("MAINSESSION", childId);
            if(res!= null){
                result = Map.of("id",res,"type","main");
            }
        }
        return result;
    }
    public String getSessionParentByChildId(String tableName, String childId){

        TraceApiTable table;
        try{
            table  = TraceApiTable.valueOf(tableName.toUpperCase());
        }catch (IllegalArgumentException e ){
            return null;
        }
        var v = new RequestQueryBuilder();
        v.select(table.table(),
                getColumns(
                         table,ID
                )
        );
        v.tables(APIREQUEST.table());
        v.filters(table.column(ID).equal(APIREQUEST.column(PARENT)).and(APIREQUEST.column(ID).equal(childId)));
        return v.build().execute(ds,(rs -> { if(rs.next()) return rs.getString(table.columnName(ID).get()); // to be changed
            return null;
        }));
    }

    public List<Session> getApiSessionById(List<String> ids, Supplier<? extends ApiRequest> fn){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(ids.toArray(String[]::new), true);
        return getApiSesssionsByCriteria(jsf, fn);
    }

    public List<Session> getApiSesssionsByCriteria(JqueryRequestSessionFilter jsf, Supplier<? extends ApiRequest> fn) {
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
            v.filters(jsf.filters(REQUEST).toArray(DBFilter[]::new));
        }
        List<Session> res = v.build().execute(ds, (rs) -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                ApiSession session = new ApiSession();
                session.setId(rs.getString(DataConstants.incReqColumns(ID)));
                session.setMethod(rs.getString(DataConstants.incReqColumns(METHOD)));
                session.setProtocol(rs.getString(DataConstants.incReqColumns(PROTOCOL)));
                session.setHost(rs.getString(DataConstants.incReqColumns(HOST)));
                session.setPort(rs.getInt(DataConstants.incReqColumns(PORT)));
                session.setPath(rs.getString(DataConstants.incReqColumns(PATH)));
                session.setQuery(rs.getString(DataConstants.incReqColumns(QUERY)));
                session.setContentType((rs.getString(DataConstants.incReqColumns(MEDIA))));
                session.setAuthScheme((rs.getString(DataConstants.incReqColumns(AUTH))));
                session.setStatus(rs.getInt(DataConstants.incReqColumns(STATUS)));
                session.setInDataSize(rs.getLong(DataConstants.incReqColumns(SIZE_IN)));
                session.setOutDataSize(rs.getLong(DataConstants.incReqColumns(SIZE_OUT)));
                session.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.incReqColumns(START))));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.incReqColumns(END))));
                session.setThreadName(rs.getString(DataConstants.incReqColumns(THREAD)));
                session.setException(new ExceptionInfo(
                        rs.getString(DataConstants.incReqColumns(ERR_TYPE)),
                        rs.getString(DataConstants.incReqColumns(ERR_MSG))
                ));
                session.setName(rs.getString(DataConstants.incReqColumns(API_NAME)));
                session.setUser(rs.getString(DataConstants.incReqColumns(USER)));
                session.setApplication(new ApplicationInfo(
                        rs.getString(DataConstants.incReqColumns(APP_NAME)),
                        rs.getString(DataConstants.incReqColumns(VERSION)),
                        rs.getString(DataConstants.incReqColumns(ADDRESS)),
                        rs.getString(DataConstants.incReqColumns(ENVIRONEMENT)),
                        rs.getString(DataConstants.incReqColumns(OS)),
                        rs.getString(DataConstants.incReqColumns(RE))
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
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds)).forEach(q -> reqMap.get(q.getParentId()).append(q));
        }
        return res;
    }

    public List<Session> getMainSessionById(String id, Supplier<? extends ApiRequest> fn){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new), true);
        return getMainSessionsByCriteria(jsf, fn);
    }

    public List<Session> getMainSessionsByCriteria(JqueryMainSessionFilter jsf, Supplier<? extends ApiRequest> fn) {
        var v = new RequestQueryBuilder();
        v.select(
                MAINSESSION.table(),
                getColumns(
                        MAINSESSION, ID, NAME, START, END, USER, OS, RE, TYPE, LOCATION, THREAD,
                        APP_NAME, VERSION, ADDRESS, ENVIRONEMENT, ERR_TYPE, ERR_MSG
                )
        );
        if(jsf != null) {
            v.filters(jsf.filters(SESSION).toArray(DBFilter[]::new));
        }
        List<Session> res = v.build().execute(ds, (rs) -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                MainSession main = new MainSession();
                main.setId(rs.getString(DataConstants.sessionColumns(ID))); // add value of nullable
                main.setName(rs.getString(DataConstants.sessionColumns(NAME)));
                main.setUser(rs.getString(DataConstants.sessionColumns(USER)));
                main.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.sessionColumns(START))));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.sessionColumns(END))));
                main.setLaunchMode(valueOfNullable(LaunchMode.class, rs.getString(DataConstants.sessionColumns(TYPE))));
                main.setLocation(rs.getString(DataConstants.sessionColumns(LOCATION)));
                main.setThreadName(rs.getString(DataConstants.sessionColumns(THREAD)));
                main.setApplication(new ApplicationInfo(
                        rs.getString(DataConstants.sessionColumns(APP_NAME)),
                        rs.getString(DataConstants.sessionColumns(VERSION)),
                        rs.getString(DataConstants.sessionColumns(ADDRESS)),
                        rs.getString(DataConstants.sessionColumns(ENVIRONEMENT)),
                        rs.getString(DataConstants.sessionColumns(OS)),
                        rs.getString(DataConstants.sessionColumns(RE))
                ));
                main.setException(new ExceptionInfo(
                        rs.getString(DataConstants.sessionColumns(ERR_TYPE)),
                        rs.getString(DataConstants.sessionColumns(ERR_MSG))
                ));
                sessions.add(main);
            }

            return sessions;
        });
        if (Objects.requireNonNull(jsf).isLazy() && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(Session::getId, identity()));
            var parentIds = reqMap.keySet().toArray(String[]::new);
            getApiRequests(parentIds, fn).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getRunnableStages(parentIds).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getDatabaseRequests(DBQUERY.column(PARENT).in(parentIds)).forEach(q -> reqMap.get(q.getParentId()).append(q));
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
        return v.build().execute(ds, (rs) -> {
            List<ApiRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                ApiRequestWrapper out = new ApiRequestWrapper(rs.getString(DataConstants.outReqColumns(PARENT)), fn);
                out.setId(rs.getString(DataConstants.outReqColumns(ID)));
                out.setProtocol(rs.getString(DataConstants.outReqColumns(PROTOCOL)));
                out.setHost(rs.getString(DataConstants.outReqColumns(HOST)));
                out.setPort(rs.getInt(DataConstants.outReqColumns(PORT)));
                out.setPath(rs.getString(DataConstants.outReqColumns(PATH)));
                out.setQuery(rs.getString(DataConstants.outReqColumns(QUERY)));
                out.setMethod(rs.getString(DataConstants.outReqColumns(METHOD)));
                out.setStatus(rs.getInt(DataConstants.outReqColumns(STATUS)));
                out.setInDataSize(rs.getLong(DataConstants.outReqColumns(SIZE_IN)));
                out.setOutDataSize(rs.getLong(DataConstants.outReqColumns(SIZE_OUT)));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.outReqColumns(START))));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.outReqColumns(END))));
                out.setThreadName(rs.getString(DataConstants.outReqColumns(THREAD)));
                out.setException(new ExceptionInfo(
                        rs.getString(DataConstants.outReqColumns(ERR_TYPE)),
                        rs.getString(DataConstants.outReqColumns(ERR_MSG))
                ));
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
        return v.build().execute(ds, (rs) -> {
            List<RunnableStageWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RunnableStageWrapper out = new RunnableStageWrapper(rs.getString(DataConstants.outStgColumns(PARENT)));
                out.setName(rs.getString(DataConstants.outStgColumns(NAME)));
                out.setLocation(rs.getString(DataConstants.outStgColumns(LOCATION)));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.outStgColumns(START))));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.outStgColumns(END))));
                out.setUser(rs.getString(DataConstants.outStgColumns(USER)));
                out.setThreadName(rs.getString(DataConstants.outStgColumns(THREAD)));
                out.setException( new ExceptionInfo(
                        rs.getString(DataConstants.outStgColumns(ERR_TYPE)),
                        rs.getString(DataConstants.outStgColumns(ERR_MSG))
                ));
                outs.add(out);
            }
            return outs;
        });
    }

    public List<DatabaseRequestWrapper> getDatabaseRequests(DBFilter filter) {
        var v = new RequestQueryBuilder();
        v.select(
                DBQUERY.table(),
                getColumns(
                        DBQUERY, ID, HOST, PORT, DB, START, END, USER, THREAD, DRIVER,
                        DB_NAME, DB_VERSION, NAME, COMMANDS, LOCATION, PARENT
                )
        );
        v.filters(filter);
        v.orders(DBQUERY.column(START).order());
        var queries = v.build().execute(ds, (rs) -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper(rs.getString(DataConstants.outQryColumns(PARENT)), rs.getLong(DataConstants.outQryColumns(ID)));
                out.setHost(rs.getString(DataConstants.outQryColumns(HOST)));
                out.setPort(rs.getInt(DataConstants.outQryColumns(PORT)));
                out.setDatabase(rs.getString(DataConstants.outQryColumns(DB)));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.outQryColumns(START))));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.outQryColumns(END))));
                out.setUser(rs.getString(DataConstants.outQryColumns(USER)));
                out.setThreadName(rs.getString(DataConstants.outQryColumns(THREAD)));
                out.setDriverVersion(rs.getString(DataConstants.outQryColumns(DRIVER)));
                out.setDatabaseName(rs.getString(DataConstants.outQryColumns(DB_NAME)));
                out.setDatabaseVersion(rs.getString(DataConstants.outQryColumns(DB_VERSION)));
                out.setLocation(rs.getString(DataConstants.outQryColumns(LOCATION)));
                out.setName(rs.getString(DataConstants.outQryColumns(NAME)));
                out.setCommands(valueOfNullabletoEnumList(SqlCommand.class, rs.getString(DataConstants.outQryColumns(COMMANDS))));
                out.setActions(new ArrayList<>());
                outs.add(out);
            }
            return outs;
        });
       /* if (!queries.isEmpty()) {
            var qMap = queries.stream().collect(toMap(DatabaseRequestWrapper::getId, identity())); //unique
            getDatabaseActions(qMap.keySet().toArray(Long[]::new)).forEach(a -> qMap.get(a.getParentId()).getActions().add(a.getAction()));
        }*/
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
        return v.build().execute(ds, (rs) -> {
            List<DatabaseActionWrapper> actions = new ArrayList<>();
            while (rs.next()) {
                actions.add(
                        new DatabaseActionWrapper(
                            rs.getLong(DataConstants.dbActColumns(PARENT)),
                            JDBCAction.valueOf(rs.getString(DataConstants.dbActColumns(TYPE))),
                            fromNullableTimestamp(rs.getTimestamp(DataConstants.dbActColumns(START))),
                            fromNullableTimestamp(rs.getTimestamp(DataConstants.dbActColumns(END))),
                            new ExceptionInfo(
                                    rs.getString(DataConstants.dbActColumns(ERR_TYPE)),
                                    rs.getString(DataConstants.dbActColumns(ERR_MSG))
                            ),
                            ofNullable(rs.getString(DataConstants.dbActColumns(ACTION_COUNT))).map(str -> Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray()).orElse(null)
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
