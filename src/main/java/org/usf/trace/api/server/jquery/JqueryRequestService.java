package org.usf.trace.api.server.jquery;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Repository;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;
import org.usf.trace.api.server.config.DataConstants;
import org.usf.trace.api.server.jquery.filter.JqueryMainSessionFilter;
import org.usf.trace.api.server.jquery.filter.JqueryRequestSessionFilter;
import org.usf.traceapi.core.*;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.*;

@Repository
@RequiredArgsConstructor
public class JqueryRequestService {

    private final DataSource ds;

    public List<Session> getIncomingRequestById(String id, boolean lazy){
        JqueryRequestSessionFilter jsf = new JqueryRequestSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return getIncomingRequestByCriteria(jsf, lazy);
    }

    public List<Session> getIncomingRequestByCriteria(JqueryRequestSessionFilter jsf, boolean lazy) {
        var v = new RequestQueryBuilder();
        v.select(
                REQUEST.table(),
                getColumns(
                        REQUEST, ID, API_NAME, APP_NAME, METHOD,
                        PROTOCOL, HOST, PORT, PATH, QUERY, MEDIA, AUTH, STATUS, SIZE_IN, SIZE_OUT,
                        START, END, THREAD, ERR_TYPE, ERR_MSG, USER, VERSION, ADDRESS, ENVIRONEMENT,
                        OS, RE
                )
        );
        if(jsf != null) {
            v.filters(jsf.filters());
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
        if (lazy && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(Session::getId, identity()));
            getOutcomingRequests(reqMap.keySet().toArray(String[]::new)).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getOutcomingStages(reqMap.keySet().toArray(String[]::new)).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getOutcomingQueries(reqMap.keySet().toArray(String[]::new)).forEach(q -> reqMap.get(q.getParentId()).append(q.getQuery()));
        }
        return res;
    }

    public List<Session> getMainSessionById(String id, boolean lazy){
        JqueryMainSessionFilter jsf = new JqueryMainSessionFilter(Collections.singletonList(id).toArray(String[]::new));
        return getMainSessionByCriteria(jsf, lazy);
    }

    public List<Session> getMainSessionByCriteria(JqueryMainSessionFilter jsf, boolean lazy) {
        var v = new RequestQueryBuilder();
        v.select(
                SESSION.table(),
                getColumns(
                        SESSION, ID, NAME, START, END, USER, OS, RE, TYPE, LOCATION, THREAD,
                        APP_NAME, VERSION, ADDRESS, ENVIRONEMENT, ERR_TYPE, ERR_MSG
                )
        );
        if(jsf != null) {
            v.filters(jsf.filters());
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
        if (lazy && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(Session::getId, identity()));
            getOutcomingRequests(reqMap.keySet().toArray(String[]::new)).forEach(r -> reqMap.get(r.getParentId()).append(r.getRequest()));
            getOutcomingStages(reqMap.keySet().toArray(String[]::new)).forEach(r -> reqMap.get(r.getParentId()).append(r.getStage()));
            getOutcomingQueries(reqMap.keySet().toArray(String[]::new)).forEach(q -> reqMap.get(q.getParentId()).append(q.getQuery()));
        }
        return res;
    }
    public List<OutcomingRequestWrapper> getOutcomingRequests(String[] incomingId) { //use criteria
        var v = new RequestQueryBuilder();
        v.select(
                OUT.table(),
                getColumns(
                        OUT, ID, PROTOCOL, HOST, PORT, PATH, QUERY, METHOD, STATUS, SIZE_IN,
                        SIZE_OUT, START, END, THREAD, ERR_TYPE, ERR_MSG, PARENT
                )
        );
        v.filters(OUT.column(PARENT).in(incomingId));
        v.orders(OUT.column(START).order());
        return v.build().execute(ds, (rs) -> {
            List<OutcomingRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                OutcomingRequestWrapper out = new OutcomingRequestWrapper(rs.getString(DataConstants.outReqColumns(PARENT)));
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

    public List<OutcomingStagesWrapper> getOutcomingStages(String[] ids){
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
            List<OutcomingStagesWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                OutcomingStagesWrapper out = new OutcomingStagesWrapper(rs.getString(DataConstants.outStgColumns(PARENT)));
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

    public List<OutcomingQueryWrapper> getOutcomingQueries(String[] ids) { // non empty
        var v = new RequestQueryBuilder();
        v.select(
                DBQUERY.table(),
                getColumns(
                        DBQUERY, ID, HOST, PORT, SCHEMA, START, END, USER, THREAD, DRIVER,
                        DB_NAME, DB_VERSION, PARENT
                )
        );
        v.filters(DBQUERY.column(PARENT).in(ids));
        v.orders(DBQUERY.column(START).order());
        var queries = v.build().execute(ds, (rs) -> {
            List<OutcomingQueryWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                OutcomingQueryWrapper out = new OutcomingQueryWrapper(rs.getLong(DataConstants.outQryColumns(ID)), rs.getString(DataConstants.outQryColumns(PARENT)));
                out.setHost(rs.getString(DataConstants.outQryColumns(HOST)));
                out.setPort(rs.getInt(DataConstants.outQryColumns(PORT)));
                out.setSchema(rs.getString(DataConstants.outQryColumns(SCHEMA)));
                out.setStart(fromNullableTimestamp(rs.getTimestamp(DataConstants.outQryColumns(START))));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp(DataConstants.outQryColumns(END))));
                out.setUser(rs.getString(DataConstants.outQryColumns(USER)));
                out.setThreadName(rs.getString(DataConstants.outQryColumns(THREAD)));
                out.setDriverVersion(rs.getString(DataConstants.outQryColumns(DRIVER)));
                out.setDatabaseName(rs.getString(DataConstants.outQryColumns(DB_NAME)));
                out.setDatabaseVersion(rs.getString(DataConstants.outQryColumns(DB_VERSION)));
                outs.add(out);
            }
            return outs;
        });
        if (!queries.isEmpty()) {
            var qMap = queries.stream().collect(toMap(OutcomingQueryWrapper::getId, identity())); //unique
            getDatabaseActions(qMap.keySet().toArray(Long[]::new)).forEach(a -> qMap.get(a.getParentId()).getActions().add(a.getAction()));
        }
        return queries;

    }

    public List<DatabaseActionWrapper> getDatabaseActions(Long[] ids) {
        var v = new RequestQueryBuilder();
        v.select(
                DBACTION.table(),
                getColumns(DBACTION, TYPE, START, END, ERR_TYPE, ERR_MSG, PARENT)
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
                            )
                        )
                );
            }
            return actions;
        });
    }

    @Getter
    static class OutcomingRequestWrapper {

        @Delegate
        private final ApiRequest request;
        private final String parentId;

        public OutcomingRequestWrapper(String parentId) {
            this.parentId = parentId;
            this.request = new ApiRequest();
        }
    }

    @Getter
    @Setter
    static class OutcomingStagesWrapper {

        @Delegate
        private final RunnableStage stage;
        private final String parentId;

        public OutcomingStagesWrapper(String parentId){
            this.parentId = parentId;
            this.stage = new RunnableStage();
        }
    }

    @Setter
    @Getter
    static class OutcomingQueryWrapper {

        @Delegate
        private final DatabaseRequest query;
        private final String parentId;
        private long id;

        public OutcomingQueryWrapper(Long id, String parentId) {
            this.parentId = parentId;
            this.id = id;
            this.query = new DatabaseRequest();
        }
    }

    @Getter
    static class DatabaseActionWrapper {

        @Delegate
        private final DatabaseAction action;
        private final long parentId;

        public DatabaseActionWrapper(long parentId, JDBCAction type, Instant start, Instant end, ExceptionInfo exception) {
            this.parentId = parentId;
            this.action = new DatabaseAction(type, start, end, exception);
        }
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
}
