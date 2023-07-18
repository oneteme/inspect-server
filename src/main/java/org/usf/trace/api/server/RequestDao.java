package org.usf.trace.api.server;

import static java.sql.Types.BIGINT;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.usf.trace.api.server.Utils.isEmpty;
import static org.usf.trace.api.server.Utils.nArg;
import static org.usf.trace.api.server.Utils.newArray;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.traceapi.core.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Repository
@RequiredArgsConstructor
public class RequestDao {

    private final JdbcTemplate template;

    @Transactional(rollbackFor = Exception.class)
    public void saveSessions(List<Session> sessions) {
        filterAndSave(sessions, IncomingRequest.class, this::addIncomingRequest);
        filterAndSave(sessions, MainRequest.class, this::addMainRequest);        
        filterSubAndSave(sessions, Session::getRequests, (s, r)-> new OutcomingRequestWrapper(r, s.getId()), this::addOutcomingRequest);
        filterSubAndSave(sessions, Session::getQueries, (s, q)-> new OutcomingQueryWrapper(q, s.getId()), this::addOutcomingQueries);
    }
    
    private void addMainRequest(List<MainRequest> reqList) {
        template.batchUpdate("INSERT INTO E_MAIN_REQ(ID_MAIN_REQ,VA_NAME,VA_USR,DH_DBT,DH_FIN,LNCH,LOC,VA_THRED,VA_APP_NME,VA_VRS,VA_ADRS,VA_ENV,VA_OS,VA_RE,VA_ERR_CLS,VA_ERR_MSG)"
        		+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var app = nullableApplication(o.getApplication());
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
            ps.setString(2, o.getName());
            ps.setString(3, o.getUser());
            ps.setTimestamp(4, fromNullableInstant(o.getStart()));
            ps.setTimestamp(5, fromNullableInstant(o.getEnd()));
            ps.setString(6, valueOfNullable(o.getLaunchMode()));
            ps.setString(7, o.getLocation());
            ps.setString(8, o.getThreadName());
            ps.setString(9, app.getName());
            ps.setString(10, app.getVersion());
            ps.setString(11, app.getAddress());
            ps.setString(12, app.getEnv());
            ps.setString(13, app.getOs());
            ps.setString(14, app.getRe());
            ps.setString(15, exp.getClassname());
            ps.setString(16, exp.getMessage());
        });
    }

    private void addIncomingRequest(List<IncomingRequest> reqList) {
        template.batchUpdate("INSERT INTO E_IN_REQ(ID_IN_REQ,VA_MTH,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_AUTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,VA_API_NME,VA_USR,VA_APP_NME,VA_VRS,VA_ADRS,VA_ENV,VA_OS,VA_RE)"
        		+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var app = nullableApplication(o.getApplication());
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
            ps.setString(2, o.getMethod());
            ps.setString(3, o.getProtocol());
            ps.setString(4, o.getHost());
            ps.setInt(5, o.getPort());
            ps.setString(6, o.getPath());
            ps.setString(7, o.getQuery());	
            ps.setString(8, o.getContentType());
            ps.setString(9, o.getAuthScheme());
            ps.setInt(10, o.getStatus());
            ps.setLong(11, o.getInDataSize());
            ps.setLong(12, o.getOutDataSize());
            ps.setTimestamp(13, fromNullableInstant(o.getStart()));
            ps.setTimestamp(14, fromNullableInstant(o.getEnd()));
            ps.setString(15, o.getThreadName());
            ps.setString(16, exp.getClassname());
            ps.setString(17, exp.getMessage());
            ps.setString(18, o.getName());
            ps.setString(19, o.getUser());
            ps.setString(20, app.getName());
            ps.setString(21, app.getVersion());
            ps.setString(22, app.getAddress());
            ps.setString(23, app.getEnv());
            ps.setString(24, app.getOs());
            ps.setString(25, app.getRe());
        });
    }

    private void addOutcomingRequest(List<OutcomingRequestWrapper> reqList) {
        template.batchUpdate("INSERT INTO E_OUT_REQ(ID_OUT_REQ,VA_MTH,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_AUTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,CD_IN_REQ)"
        		+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
            ps.setString(2, o.getMethod());
            ps.setString(3, o.getProtocol());
            ps.setString(4, o.getHost());
            ps.setInt(5, o.getPort());
            ps.setString(6, o.getPath());
            ps.setString(7, o.getQuery());
            ps.setString(8, o.getContentType());
            ps.setString(9, o.getAuthScheme());
            ps.setInt(10, o.getStatus());
            ps.setLong(11, o.getInDataSize());
            ps.setLong(12, o.getOutDataSize());
            ps.setTimestamp(13, fromNullableInstant(o.getStart()));
            ps.setTimestamp(14, fromNullableInstant(o.getEnd()));
            ps.setString(15, o.getThreadName());
            ps.setString(16,exp.getClassname());
            ps.setString(17,exp.getMessage());
            ps.setString(18, o.getParentId());
        });
    }

    private void addOutcomingQueries(List<OutcomingQueryWrapper> qryList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Long.class);
        var inc = new AtomicLong(maxId);
        template.batchUpdate("INSERT INTO E_OUT_QRY(ID_OUT_QRY,VA_HST,VA_SCHMA,DH_DBT,DH_FIN,VA_USR,VA_THRED,VA_DRV,VA_DB_NME,VA_DB_VRS,VA_CMPLT,CD_IN_REQ)"
        		+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", qryList, qryList.size(), (ps, o) -> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setString(3, o.getSchema());
            ps.setTimestamp(4, fromNullableInstant(o.getStart()));
            ps.setTimestamp(5, fromNullableInstant(o.getEnd())); 
            ps.setString(6, o.getUser());
            ps.setString(7, o.getThreadName());
            ps.setString(8, o.getDriverVersion());
            ps.setString(9, o.getDatabaseName());
            ps.setString(10, o.getDatabaseVersion());
            ps.setString(11, o.isCompleted() ? "T" : "F");
            ps.setString(12, o.getParentId());
            o.setId(inc.get());
        });
        addDatabaseActions(qryList);
    }

    private void addDatabaseActions(List<OutcomingQueryWrapper> queries) {
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_ERR_CLS,VA_ERR_MSG,CD_OUT_QRY) VALUES(?,?,?,?,?,?)",
                queries.stream()
                        .flatMap(e -> e.getActions().stream().map(da -> {
                                    var exp = nullableException(da.getException());
                                    return new Object[]{da.getType().toString(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), exp.getClassname(), exp.getMessage(), e.getId()
                                    };
                                }
                        ))
                        .collect(toList()),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, VARCHAR, BIGINT});
    }

    @Deprecated // reuse RequestDao::outcomingRequests using criteria
    public OutcomingRequest getOutcomingRequestById(String id) {
        return template.query("SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ"
        		+ " WHERE ID_OUT_REQ = ? ", new Object[]{id}, newArray(1, VARCHAR), rs -> {

            if(rs.next()) {
                OutcomingRequest out = new OutcomingRequest(rs.getString("ID_OUT_REQ"));
                out.setProtocol(rs.getString("VA_PRTCL"));
                out.setHost(rs.getString("VA_HST"));
                out.setPort(rs.getInt("CD_PRT"));
                out.setPath(rs.getString("VA_PTH"));
                out.setQuery(rs.getString("VA_QRY"));
                out.setMethod(rs.getString("VA_MTH"));
                out.setStatus(rs.getInt("CD_STT"));
                out.setInDataSize(rs.getLong("VA_I_SZE"));
                out.setOutDataSize(rs.getLong("VA_O_SZE"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("DH_DBT")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("DH_FIN")));
                out.setThreadName(rs.getString("VA_THRED"));
                return out;
            }
            return null;
        });
    }

    public List<MainRequest> getMainRequestById(boolean lazy, String... idArr) {
        var query = "SELECT ID_MAIN_REQ,VA_NAME,VA_USR,DH_DBT,DH_FIN,LNCH,LOC,VA_THRED,VA_APP_NME,VA_VRS,VA_ADRS,VA_ENV,VA_OS,VA_RE,VA_ERR_CLS,VA_ERR_MSG FROM E_MAIN_REQ";
        int[] argTypes = null;
        if(!isEmpty(idArr)) {
            query += " WHERE ID_MAIN_REQ IN(" + nArg(idArr.length) + ")";
            argTypes = newArray(idArr.length, VARCHAR);
        }
        List<MainRequest> res = template.query(query, idArr, argTypes, (rs, i) -> {
            MainRequest main = new MainRequest(rs.getString("ID_MAIN_REQ"));
            main.setName(rs.getString("VA_NAME"));
            main.setUser(rs.getString("VA_USR"));
            main.setStart(fromNullableTimestamp(rs.getTimestamp("DH_DBT")));
            main.setEnd(fromNullableTimestamp(rs.getTimestamp("DH_FIN")));
            main.setLaunchMode(valueOfNullable(LaunchMode.class, rs.getString("LNCH")));
            main.setLocation(rs.getString("LOC"));
            main.setThreadName(rs.getString("VA_THRED"));
            main.setApplication(new ApplicationInfo(
            		rs.getString("VA_APP_NME"),
                    rs.getString("VA_VRS"),
                    rs.getString("VA_ADRS"),
                    rs.getString("VA_ENV"),
                    rs.getString("VA_OS"),
                    rs.getString("VA_RE")
            ));
            main.setException(new ExceptionInfo(
                    rs.getString("VA_ERR_CLS"),
                    rs.getString("VA_ERR_MSG")
            ));
            return main;
        });
        if(lazy && !res.isEmpty()) {
            var reqMap = res.stream().collect(toMap(MainRequest::getId, identity()));
            outcomingRequests(reqMap.keySet()).forEach(r-> reqMap.get(r.getParentId()).getRequests().add(r.getRequest()));
            outcomingQueries(reqMap.keySet()).forEach(q-> reqMap.get(q.getParentId()).getQueries().add(q.getQuery()));
        }
        return res;
    }
    public List<IncomingRequest> getIncomingRequestById(boolean lazy, String... idArr) {
        var query = "SELECT ID_IN_REQ,VA_MTH,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_AUTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,VA_API_NME,VA_USR,VA_APP_NME,VA_VRS,VA_ADRS,VA_ENV,VA_OS,VA_RE FROM E_IN_REQ";
        int[] argTypes = null;
        if(!isEmpty(idArr)) {
            query += " WHERE ID_IN_REQ IN(" + nArg(idArr.length) + ")";
            argTypes = newArray(idArr.length, VARCHAR);
        }
        List<IncomingRequest> res = template.query(query, idArr, argTypes, (rs, i) -> {
            IncomingRequest in = new IncomingRequest(rs.getString("ID_IN_REQ"));
            in.setMethod(rs.getString("VA_MTH"));
            in.setProtocol(rs.getString("VA_PRTCL"));
            in.setHost(rs.getString("VA_HST"));
            in.setPort(rs.getInt("CD_PRT"));
            in.setPath(rs.getString("VA_PTH"));
            in.setQuery(rs.getString("VA_QRY"));
            in.setContentType((rs.getString("VA_CNT_TYP")));
            in.setAuthScheme((rs.getString("VA_AUTH")));
            in.setStatus(rs.getInt("CD_STT"));
            in.setInDataSize(rs.getLong("VA_I_SZE"));
            in.setOutDataSize(rs.getLong("VA_I_SZE"));
            in.setStart(fromNullableTimestamp(rs.getTimestamp("DH_DBT")));
            in.setEnd(fromNullableTimestamp(rs.getTimestamp("DH_FIN")));
            in.setThreadName(rs.getString("VA_THRED"));
            in.setException(new ExceptionInfo(
                    rs.getString("VA_ERR_CLS"),
                    rs.getString("VA_ERR_MSG")
            ));
            in.setName(rs.getString("VA_API_NME"));
            in.setUser(rs.getString("VA_USR"));
            in.setApplication(new ApplicationInfo(
            		rs.getString("VA_APP_NME"),
                    rs.getString("VA_VRS"),
                    rs.getString("VA_ADRS"),
                    rs.getString("VA_ENV"),
            		rs.getString("VA_OS"),
            		rs.getString("VA_RE")
            ));
            return in;
        });
        if(lazy && !res.isEmpty()) {
        	var reqMap = res.stream().collect(toMap(IncomingRequest::getId, identity()));
            outcomingRequests(reqMap.keySet()).forEach(r-> reqMap.get(r.getParentId()).getRequests().add(r.getRequest()));
            outcomingQueries(reqMap.keySet()).forEach(q-> reqMap.get(q.getParentId()).getQueries().add(q.getQuery()));
        }
        return res;
    }

    public List<OutcomingRequestWrapper> outcomingRequests(Set<String> incomingId) { //use criteria 
        var query = "SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,CD_IN_REQ FROM E_OUT_REQ"
        		+ " WHERE CD_IN_REQ IN(" + nArg(incomingId.size()) + ")";
        return template.query(query, incomingId.toArray(), newArray(incomingId.size(), VARCHAR), (rs, i) -> {
            OutcomingRequestWrapper out = new OutcomingRequestWrapper(rs.getString("ID_OUT_REQ"), rs.getString("CD_IN_REQ"));
            out.setProtocol(rs.getString("VA_PRTCL"));
            out.setHost(rs.getString("VA_HST"));
            out.setPort(rs.getInt("CD_PRT"));
            out.setPath(rs.getString("VA_PTH"));
            out.setQuery(rs.getString("VA_QRY"));
            out.setMethod(rs.getString("VA_MTH"));
            out.setStatus( rs.getInt("CD_STT"));
            out.setInDataSize(rs.getLong("VA_I_SZE"));
            out.setOutDataSize(rs.getLong("VA_I_SZE"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("DH_DBT")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("DH_FIN")));
            out.setThreadName(rs.getString("VA_THRED"));
            out.setException(new ExceptionInfo(
                    rs.getString("VA_ERR_CLS"),
                    rs.getString("VA_ERR_MSG")
            ));

            return out;
        });
    }

    public List<OutcomingQueryWrapper> outcomingQueries(Set<String> incomingId) { // non empty
        var query = "SELECT ID_OUT_QRY,VA_HST,CD_PRT,VA_SCHMA,DH_DBT,DH_FIN,VA_USR,VA_THRED,VA_DRV,VA_DB_NME,VA_DB_VRS,VA_CMPLT,CD_IN_REQ FROM E_OUT_QRY"
        		+ " WHERE CD_IN_REQ IN(" + nArg(incomingId.size()) + ")";
        var queries = template.query(query, incomingId.toArray(), newArray(incomingId.size(), VARCHAR), (rs, i) -> {
            var out = new OutcomingQueryWrapper(rs.getLong("ID_OUT_QRY"), rs.getString("CD_IN_REQ"));
            out.setHost(rs.getString("VA_HST"));
            out.setPort(rs.getInt("CD_PRT"));
            out.setSchema(rs.getString("VA_SCHMA"));
            out.setStart(fromNullableTimestamp(rs.getTimestamp("DH_DBT")));
            out.setEnd(fromNullableTimestamp(rs.getTimestamp("DH_FIN")));
            out.setUser(rs.getString("VA_USR"));
            out.setThreadName(rs.getString("VA_THRED"));
            out.setDriverVersion(rs.getString("VA_DRV"));
            out.setDatabaseName(rs.getString("VA_DB_NME"));
            out.setDatabaseVersion(rs.getString("VA_DB_VRS"));
            out.setCompleted("T".equals(rs.getString("VA_CMPLT")));
            return out;
        });
        if(!queries.isEmpty()) {
        	var qMap = queries.stream().collect(toMap(OutcomingQueryWrapper::getId, identity())); //unique 
            databaseActions(qMap.keySet()).forEach(a-> qMap.get(a.getParentId()).getActions().add(a.getAction()));
        }
        return queries;
    }

    public List<DatabaseActionWrapper> databaseActions(Set<Long> queries) { // non empty
        var query = "SELECT VA_TYP,DH_DBT,DH_FIN,VA_ERR_CLS,VA_ERR_MSG,CD_OUT_QRY FROM E_DB_ACT"
        		+ " WHERE CD_OUT_QRY IN(" + nArg(queries.size()) + ")";
        return template.query(query, queries.toArray(), newArray(queries.size(), BIGINT), (rs, i)->
                new DatabaseActionWrapper(
                        rs.getLong("CD_OUT_QRY"),
                        Action.valueOf(rs.getString("VA_TYP")),
                        rs.getTimestamp("DH_DBT").toInstant(),
                        ofNullable(rs.getTimestamp("DH_FIN")).map(Timestamp::toInstant).orElse(null),
                        new ExceptionInfo(
                                rs.getString("VA_ERR_CLS"),
                                rs.getString("VA_ERR_MSG")
                        )));
    }

    @Getter
    class OutcomingRequestWrapper {
        
        @Delegate
    	private final OutcomingRequest request;
        private final String parentId;

        public OutcomingRequestWrapper(String id, String parentId) {
            this.parentId = parentId;
            this.request  = new OutcomingRequest(id); //delegated setters
        }
        
        public OutcomingRequestWrapper(OutcomingRequest request, String parentId) {
            this.parentId = parentId;
            this.request  = request; //delegated getters
        }
    }

    @Setter
    @Getter
    class OutcomingQueryWrapper {
        
        @Delegate
        private final OutcomingQuery query;
        private final String parentId;
        private long id;

        public OutcomingQueryWrapper(Long id, String parentId) {
            this.parentId = parentId;
            this.id = id;
            this.query = new OutcomingQuery(); //delegated setters
        }
        
        public OutcomingQueryWrapper(OutcomingQuery query, String parentId) {
            this.parentId = parentId;
            this.query = query; //delegated getters
        }
    }

    @Getter
    class DatabaseActionWrapper  {

    	@Delegate
    	private final DatabaseAction action;
    	private final long parentId;

        public DatabaseActionWrapper(long parentId, Action type, Instant start, Instant end, ExceptionInfo exception) {
            this.parentId = parentId;
            this.action = new DatabaseAction(type, start, end, exception);
        }
    }

    private static <T, U extends T> void filterAndSave(Collection<T> c, Class<U> classe, Consumer<List<U>> saveFn){
    	var list = c.stream()
    			.filter(classe::isInstance)
    			.map(classe::cast)
    			.collect(toList());
    	if(!list.isEmpty()) {
    		saveFn.accept(list);
    	}
    }
    
    private static <T, U, R> void filterSubAndSave(Collection<T> c, Function<T, Collection<U>> accessor, BiFunction<T, U, R> mapper, Consumer<List<R>> saveFn){
    	var list = c.stream()
    			.filter(o-> nonNull(accessor.apply(o)))
    			.flatMap(o-> accessor.apply(o).stream().map(s-> mapper.apply(o, s)))
    			.collect(toList());
    	if(!list.isEmpty()) {
    		saveFn.accept(list);
    	}
    }
    
    private static Timestamp fromNullableInstant(Instant instant) {
    	return ofNullable(instant).map(Timestamp::from).orElse(null);
    }
    
    private static Instant fromNullableTimestamp(Timestamp timestamp) {
    	return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
    }
    
    private static ApplicationInfo nullableApplication(ApplicationInfo app) {
    	return ofNullable(app).orElseGet(()-> new ApplicationInfo(null, null, null, null, null, null));
    }

    private static ExceptionInfo nullableException(ExceptionInfo exp){
        return ofNullable(exp).orElseGet(()-> new ExceptionInfo(null, null));
    }

    private static String valueOfNullable(Object o) {
    	return ofNullable(o).map(Object::toString).orElse(null);
    }

    private static <T extends Enum<T>> T valueOfNullable(Class<T> classe, String value) {
    	return ofNullable(value)
    			.flatMap(v-> Stream.of(classe.getEnumConstants()).filter(e-> e.name().equals(v)).findAny())
    			.orElse(null);
    }
}