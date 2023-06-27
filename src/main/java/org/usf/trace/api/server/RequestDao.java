package org.usf.trace.api.server;

import static java.sql.Timestamp.from;
import static java.sql.Types.BIGINT;
import static java.sql.Types.CHAR;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.usf.trace.api.server.Utils.isEmpty;
import static org.usf.trace.api.server.Utils.nArg;
import static org.usf.trace.api.server.Utils.newArray;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.traceapi.core.Action;
import org.usf.traceapi.core.DatabaseAction;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingQuery;
import org.usf.traceapi.core.OutcomingRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;

@Repository
@RequiredArgsConstructor
public class RequestDao {

    private final JdbcTemplate template;

    @Transactional(rollbackFor = Exception.class)
    public void addIncomingRequest(List<IncomingRequest> reqList) {
        List<OutcomingRequestWrapper> outreq = new LinkedList<>();
        List<OutcomingQueryWrapper> outqry = new LinkedList<>();
        template.batchUpdate("INSERT INTO E_IN_REQ(ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getProtocol());
            ps.setString(3, o.getHost());
            ps.setInt(4, o.getPort());
            ps.setString(5, o.getPath());
            ps.setString(6, o.getQuery());
            ps.setString(7, o.getMethod());
            ps.setInt(8, o.getStatus());
            ps.setLong(9, o.getInDataSize());
            ps.setLong(10, o.getOutDataSize());
            ps.setTimestamp(11, from(o.getStart()));
            ps.setTimestamp(12, from(o.getEnd()));
            ps.setString(13, o.getThread());
            ps.setString(14, o.getContentType());
            ps.setString(15, o.getEndpoint());
            ps.setString(16, o.getResource());
            ps.setString(17, o.getClient());
            ps.setString(18, o.getGroup());
        	o.getRequests().forEach(or-> outreq.add(new OutcomingRequestWrapper(or, o.getId())));
        	o.getQueries().forEach(oq-> outqry.add(new OutcomingQueryWrapper(oq, o.getId())));
        });
        addOutcomingRequest(outreq);
        addOutcomingQueries(outqry);
    }

    private void addOutcomingRequest(List<OutcomingRequestWrapper> reqList) {
        template.batchUpdate("INSERT INTO E_OUT_REQ(ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getProtocol());
            ps.setString(3, o.getHost());
            ps.setInt(4, o.getPort());
            ps.setString(5, o.getPath());
            ps.setString(6, o.getQuery());
            ps.setString(7, o.getMethod());
            ps.setInt(8, o.getStatus() == null ? -1 : o.getStatus());
            ps.setLong(9, o.getInDataSize());
            ps.setLong(10, o.getOutDataSize());
            ps.setTimestamp(11, from(o.getStart()));
            ps.setTimestamp(12, from(o.getEnd()));
            ps.setString(13, o.getThread());
            ps.setString(14, o.getParentId());
        });
    }

    private void addOutcomingQueries(List<OutcomingQueryWrapper> qryList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Long.class);
        var inc = new AtomicLong(maxId);
        template.batchUpdate("INSERT INTO E_OUT_QRY(ID_OUT_QRY,VA_HST,VA_SCHMA,DH_DBT,DH_FIN,VA_THRED,VA_FAIL,CD_IN_REQ) VALUES(?,?,?,?,?,?,?,?)", qryList, qryList.size(), (ps, o) -> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setString(3, o.getSchema());
            ps.setTimestamp(4, from(o.getStart()));
            ps.setTimestamp(5, from(o.getEnd()));
            ps.setString(6, o.getThread());
            ps.setString(7, o.isFailed() ? "T" : "F");
            ps.setString(8, o.getParentId());
            o.setId(inc.get());
        });
        addDatabaseActions(qryList);
    }

    private void addDatabaseActions(List<OutcomingQueryWrapper> queries) {
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY) VALUES(?,?,?,?,?)", 
        		queries.stream()
    			.flatMap(e -> e.getActions().stream().map(da ->
    				new Object[]{da.getType().toString(), from(da.getStart()), from(da.getEnd()), da.isFailed() ? "T" : "F", e.getId()}))
    			.collect(toList()), 
        		new int[] {VARCHAR, TIMESTAMP, TIMESTAMP, CHAR, BIGINT});
    }

    @Deprecated // reuse  RequestDao::outcomingRequests using criteria 
    public OutcomingRequest getOutcomingRequestById(String id) {
        return template.query("SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ WHERE ID_OUT_REQ = ? ", new Object[]{id}, newArray(1, VARCHAR), rs -> {

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
                out.setStart(rs.getTimestamp("DH_DBT").toInstant());
                out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
                out.setThread(rs.getString("VA_THRED"));
                return out;
            }
            return null;
        });
    }

    public List<IncomingRequest> getIncomingRequestById(boolean lazy, String... idArr) {
        var query = "SELECT ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP FROM E_IN_REQ";
        int[] argTypes = null;
        if(!isEmpty(idArr)) {
            query += " WHERE ID_IN_REQ IN(" + nArg(idArr.length) + ")";
            argTypes = newArray(idArr.length, VARCHAR);
        }
        List<IncomingRequest> res = template.query(query, idArr, argTypes, (rs, i) -> {
            IncomingRequest in = new IncomingRequest(rs.getString("ID_IN_REQ"));
            in.setProtocol(rs.getString("VA_PRTCL"));
            in.setHost(rs.getString("VA_HST"));
            in.setPort(rs.getInt("CD_PRT"));
            in.setPath(rs.getString("VA_PTH"));
            in.setQuery(rs.getString("VA_QRY"));
            in.setMethod(rs.getString("VA_MTH"));
            in.setStatus(rs.getInt("CD_STT"));
            in.setInDataSize(rs.getLong("VA_I_SZE"));
            in.setOutDataSize(rs.getLong("VA_I_SZE"));
            in.setStart(rs.getTimestamp("DH_DBT").toInstant());
            in.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            in.setThread(rs.getString("VA_THRED"));
            in.setContentType(rs.getString("VA_CNT_TYP"));
            in.setEndpoint(rs.getString("VA_ACT"));
            in.setResource(rs.getString("VA_RSC"));
            in.setClient(rs.getString("VA_CLI"));
            in.setGroup(rs.getString("VA_GRP"));
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
        var query = "SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ"
        		+ " WHERE CD_IN_REQ IN(" + nArg(incomingId.size()) + ")";
        return template.query(query, incomingId.toArray(), newArray(incomingId.size(), VARCHAR), (rs, i) -> {
            OutcomingRequestWrapper out = new OutcomingRequestWrapper(rs.getString("ID_OUT_REQ"), rs.getString("CD_IN_REQ"));
            out.setProtocol(rs.getString("VA_PRTCL"));
            out.setHost(rs.getString("VA_HST"));
            out.setPort(rs.getInt("CD_PRT"));
            out.setPath(rs.getString("VA_PTH"));
            out.setQuery(rs.getString("VA_QRY"));
            out.setMethod(rs.getString("VA_MTH"));
            out.setStatus(rs.getInt("CD_STT"));
            out.setInDataSize(rs.getLong("VA_I_SZE"));
            out.setOutDataSize(rs.getLong("VA_I_SZE"));
            out.setStart(rs.getTimestamp("DH_DBT").toInstant());
            out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            out.setThread(rs.getString("VA_THRED"));
            return out;
        });
    }

    public List<OutcomingQueryWrapper> outcomingQueries(Set<String> incomingId) { // non empty
        var query = "SELECT ID_OUT_QRY,VA_HST,VA_SCHMA,DH_DBT,DH_FIN,VA_THRED,VA_FAIL,CD_IN_REQ FROM E_OUT_QRY" 
        		+ " WHERE CD_IN_REQ IN(" + nArg(incomingId.size()) + ")";
        var queries = template.query(query, incomingId.toArray(), newArray(incomingId.size(), VARCHAR), (rs, i) -> {
            var out = new OutcomingQueryWrapper(rs.getLong("ID_OUT_QRY"), rs.getString("CD_IN_REQ"));
            out.setHost(rs.getString("VA_HST"));
            out.setSchema(rs.getString("VA_SCHMA"));
            out.setStart(rs.getTimestamp("DH_DBT").toInstant());
            out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            out.setThread(rs.getString("VA_THRED"));
            out.setFailed("T".equals(rs.getString("VA_FAIL")));
            return out;
        });
        if(!queries.isEmpty()) {
        	var qMap = queries.stream().collect(toMap(OutcomingQueryWrapper::getId, identity())); //unique 
            databaseActions(qMap.keySet()).forEach(a-> qMap.get(a.getParentId()).getActions().add(a.getAction()));
        }
        return queries;
    }

    public List<DatabaseActionWrapper> databaseActions(Set<Long> queries) { // non empty
        var query = "SELECT VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY FROM E_DB_ACT"
        		+ " WHERE CD_OUT_QRY IN(" + nArg(queries.size()) + ")";
        return template.query(query, queries.toArray(), newArray(queries.size(), BIGINT), (rs, i)->
                new DatabaseActionWrapper(
                        rs.getLong("CD_OUT_QRY"),
                        Action.valueOf(rs.getString("VA_TYP")),
                        rs.getTimestamp("DH_DBT").toInstant(),
                        rs.getTimestamp("DH_FIN").toInstant(),
                        "T".equals(rs.getString("VA_FAIL"))));
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

        public DatabaseActionWrapper(long parentId, Action type, Instant start, Instant end, boolean failed) {
            this.parentId = parentId;
            this.action = new DatabaseAction(type, start, end, failed);
        }
    }
}