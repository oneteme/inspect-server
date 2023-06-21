package org.usf.trace.api.server;

import static java.sql.Timestamp.from;
import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.usf.trace.api.server.Utils.isEmpty;
import static org.usf.trace.api.server.Utils.nArg;
import static org.usf.trace.api.server.Utils.newArray;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.traceapi.core.Action;
import org.usf.traceapi.core.DatabaseAction;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingQuery;
import org.usf.traceapi.core.OutcomingRequest;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RequestDao {

    private final JdbcTemplate template;

    @Transactional(rollbackFor = Exception.class)
    public void addIncomingRequest(List<IncomingRequest> reqList) {
        List<ServerOutcomingRequest> outreq = new LinkedList<>();
        List<ServerOutcomingQuery> outqry = new LinkedList<>();
        template.batchUpdate("INSERT INTO E_IN_REQ (ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
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

            for (OutcomingRequest or : o.getRequests()) {
                outreq.add(mapServerOutcomingRequest(or, o.getId()));
            }
            for (OutcomingQuery oq : o.getQueries()) {
                outqry.add(mapServerOutcomingQuery(oq, o.getId()));
            }

        });
        addOucomingRequest(outreq);
        addOutcomingQueries(outqry);
    }

    private void addOucomingRequest(List<ServerOutcomingRequest> reqList) {
        template.batchUpdate("INSERT INTO E_OUT_REQ (ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
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
            ps.setString(14, o.idIncoming);
        });
    }

    private void addOutcomingQueries(List<ServerOutcomingQuery> qryList) {
        final Integer maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Integer.class);
        var map = new LinkedHashMap<OutcomingQuery, Integer>(); // to be changed
        AtomicInteger inc = new AtomicInteger(maxId);
        template.batchUpdate("INSERT INTO E_OUT_QRY (ID_OUT_QRY,VA_HST,VA_SCHMA,DH_DBT,DH_FIN,VA_THRED,VA_FAIL,CD_IN_REQ) VALUES  (?,?,?,?,?,?,?,?)", qryList, qryList.size(), (ps, o) -> {
            ps.setInt(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setString(3, o.getSchema());
            ps.setTimestamp(4, from(o.getStart()));
            ps.setTimestamp(5, from(o.getEnd()));
            ps.setString(6, o.getThread());
            ps.setInt(7, Boolean.compare(o.isFailed(), false));
            ps.setString(8, o.getIdIncoming());
            map.put(o, inc.get());
        });

        addDatabaseAction(map);
    }

    private void addDatabaseAction(Map<OutcomingQuery, Integer> map) { // to be changed
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY) VALUES (?,?,?,?,?)", map.entrySet().stream().flatMap(e ->
                e.getKey().getActions().stream().map(da ->
                        new Object[]{da.getType().toString(), from(da.getStart()), from(da.getEnd()), Boolean.compare(da.isFailed(), false), e.getValue()})
        ).collect(toList()));
    }

    public OutcomingRequest getOutcomingRequestById(String id) {
        return template.query("SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ WHERE ID_OUT_REQ = ? ", new Object[]{id}, newArray(1, VARCHAR), rs -> {

            if (rs.next()) {
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
        var query = "SELECT ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP FROM E_IN_REQ ";
        int[] argTypes = null;
        if (!isEmpty(idArr)) {
            query += "WHERE ID_IN_REQ IN (" + nArg(idArr.length) + ")";
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
        if (lazy && !res.isEmpty()) {
            var outReqMap = getOutcomingRequestListForInReq(idArr).stream().collect(groupingBy(ServerOutcomingRequest::getIdIncoming));
            var outQryMap = getOutcomingQueryListForInReq(idArr).stream().collect(groupingBy(ServerOutcomingQuery::getIdIncoming));
            for (IncomingRequest in : res) {
                if (outQryMap.containsKey(in.getId())) {
                	in.getQueries().addAll(outQryMap.get(in.getId()));
                }
                if (outReqMap.containsKey(in.getId())) {
                	in.getRequests().addAll(outReqMap.get(in.getId()));
                }
            }
        }
        return res;

    }

    public List<ServerOutcomingRequest> getOutcomingRequestListForInReq(String... idArr) {

        var query = "SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_I_SZE,VA_O_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ ";
        int[] argTypes = null;
        if (!isEmpty(idArr)) {
            query += "WHERE CD_IN_REQ IN (" + nArg(idArr.length) + ")";
            argTypes = newArray(idArr.length, VARCHAR);
        }
        return template.query(query, idArr, argTypes, (rs, i) -> {
            ServerOutcomingRequest out = new ServerOutcomingRequest(rs.getString("ID_OUT_REQ"), rs.getString("CD_IN_REQ"));
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

    public List<ServerOutcomingQuery> getOutcomingQueryListForInReq(String... idArr) {
        var query = "SELECT ID_OUT_QRY,VA_HST,VA_SCHMA,DH_DBT,DH_FIN,VA_THRED,VA_FAIL,CD_IN_REQ FROM E_OUT_QRY ";
        int[] argTypes = null;
        if (!isEmpty(idArr)) {
            query += "WHERE CD_IN_REQ IN (" + nArg(idArr.length) + ")";
            argTypes = newArray(idArr.length, VARCHAR);
        }
        List<Long> idQryArr = new LinkedList<>();
        List<ServerOutcomingQuery> outList = template.query(query, idArr, argTypes, (rs, i) -> {
            ServerOutcomingQuery out = new ServerOutcomingQuery(rs.getLong("ID_OUT_QRY"), rs.getString("CD_IN_REQ"));
            out.setHost(rs.getString("VA_HST"));
            out.setSchema(rs.getString("VA_SCHMA"));
            out.setStart(rs.getTimestamp("DH_DBT").toInstant());
            out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            out.setThread(rs.getString("VA_THRED"));
            out.setFailed((rs.getInt("VA_FAIL") != 0));
            idQryArr.add(rs.getLong("ID_OUT_QRY"));
            return out;
        });

        var dataMap = getDatabaseActionListForOutReq1(idQryArr).stream().collect(groupingBy(ServerDatabaAction::getId));
        for (ServerOutcomingQuery in : outList) {
            if (dataMap.containsKey(in.getIdOutQry()))
                in.getActions().addAll(dataMap.get(in.getIdOutQry()));
        }
        return outList;
    }

    public List<ServerDatabaAction> getDatabaseActionListForOutReq1(List<Long> queries) {

        var query = "SELECT VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY FROM E_DB_ACT ";
        int[] argTypes = null;
        if (!isEmpty(queries)) {
            query += "WHERE CD_OUT_QRY IN (" + nArg(queries.size()) + ")";
            argTypes = newArray(queries.size(), BIGINT);
        }
        return template.query(query, queries.toArray(), argTypes, (rs, i) ->
                new ServerDatabaAction(
                        rs.getLong("CD_OUT_QRY"),
                        Action.valueOf(rs.getString("VA_TYP")),
                        rs.getTimestamp("DH_DBT").toInstant(),
                        rs.getTimestamp("DH_FIN").toInstant(),
                        (rs.getInt("VA_FAIL") != 0)));
    }

    public ServerOutcomingRequest mapServerOutcomingRequest(OutcomingRequest or, String incomingRequestId) {
        ServerOutcomingRequest outr = new ServerOutcomingRequest(or.getId(), incomingRequestId);
        outr.setProtocol(or.getProtocol());
        outr.setHost(or.getHost());
        outr.setPort(or.getPort());
        outr.setPath(or.getPath());
        outr.setQuery(or.getQuery());
        outr.setMethod(or.getMethod());
        outr.setStatus(or.getStatus());
        outr.setInDataSize(or.getInDataSize());
        outr.setOutDataSize(or.getOutDataSize());
        outr.setStart(or.getStart());
        outr.setEnd(or.getEnd());
        outr.setThread(or.getThread());
        return outr;
    }

    public ServerOutcomingQuery mapServerOutcomingQuery(OutcomingQuery oq, String incomingRequestId) {
        ServerOutcomingQuery outq = new ServerOutcomingQuery(null, incomingRequestId);
        outq.setHost(oq.getHost());
        outq.setSchema(oq.getSchema());
        outq.setStart(oq.getStart());
        outq.setEnd(oq.getEnd());
        outq.setThread(oq.getThread());
        outq.getActions().addAll(oq.getActions());
        return outq;
    }

    @Getter
    static class ServerOutcomingRequest extends OutcomingRequest {
        @JsonIgnore
        private final String idIncoming;

        public ServerOutcomingRequest(String id, String idIncoming) {
            super(id);
            this.idIncoming = idIncoming;
        }
    }

    @Getter
    static class ServerOutcomingQuery extends OutcomingQuery {
        @JsonIgnore
        private final String idIncoming;

        @JsonIgnore
        private final Long idOutQry;

        public ServerOutcomingQuery(Long idOutQry, String idIncoming) {
            this.idIncoming = idIncoming;
            this.idOutQry = idOutQry;
        }
    }

    @Getter
    static class ServerDatabaAction extends DatabaseAction {
        @JsonIgnore
        private final long id;

        public ServerDatabaAction(long id, Action type, Instant start, Instant end, boolean failed) {
            super(type, start, end, failed);
            this.id = id;
        }
    }


}