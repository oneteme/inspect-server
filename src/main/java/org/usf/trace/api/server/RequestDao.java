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
    public IncomingRequest addIncomingRequest(IncomingRequest req) {
        template.update("INSERT INTO E_IN_REQ (ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                req.getId(),
                req.getProtocol(),
                req.getHost(),
                req.getPort(),
                req.getPath(),
                req.getQuery(),
                req.getMethod(),
                req.getStatus(),
                req.getSize(),
                req.getStart(),
                req.getEnd(),
                req.getThread(),
                req.getContentType(),
                req.getEndpoint(),
                req.getResource(),
                req.getClient(),
                req.getGroup());
        addOucomingRequest(req.getRequests(), req.getId());
        addOutcomingQueries(req.getQueries(), req.getId());
        return req;
    }

    private void addOucomingRequest(Collection<OutcomingRequest> reqList, String incomingRequestId) {
        template.batchUpdate("INSERT INTO E_OUT_REQ (ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'" + incomingRequestId + "')", reqList, reqList.size(), (ps, o) -> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getProtocol());
            ps.setString(3, o.getHost());
            ps.setInt(4, o.getPort());
            ps.setString(5, o.getPath());
            ps.setString(6, o.getQuery());
            ps.setString(7, o.getMethod());
            ps.setInt(8, o.getStatus());
            ps.setLong(9, o.getSize());
            ps.setTimestamp(10, from(o.getStart()));
            ps.setTimestamp(11, from(o.getEnd()));
            ps.setString(12, o.getThread());
        });
    }

    private void addOutcomingQueries(Collection<OutcomingQuery> qryList, String incomingRequestId) {
        final Integer maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Integer.class);
        var map = new LinkedHashMap<OutcomingQuery, Integer>();
        AtomicInteger inc = new AtomicInteger(maxId);
        template.batchUpdate("INSERT INTO E_OUT_QRY (ID_OUT_QRY,VA_URL,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ) VALUES  (?,?,?,?,?,'" + incomingRequestId + "')", qryList, qryList.size(), (ps, o) -> {
            ps.setInt(1, inc.incrementAndGet());
            ps.setString(2, o.getUrl());
            ps.setTimestamp(3, from(o.getStart()));
            ps.setTimestamp(4, from(o.getEnd()));
            ps.setString(5, o.getThread());
            map.put(o, inc.get());
        });
        addDatabaseAction(map);
    }

    private void addDatabaseAction(Map<OutcomingQuery, Integer> map) {
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY) VALUES (?,?,?,?,?)", map.entrySet().stream().flatMap(e ->
                e.getKey().getActions().stream().map(da ->
                        new Object[]{da.getType().toString(), from(da.getStart()), from(da.getEnd()), da.isFailed(), e.getValue()})
        ).collect(toList()));
    }

    public List<IncomingRequest> getIncomingRequestById(boolean getSubList, String... idArr) {
        var query = "SELECT ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP FROM E_IN_REQ ";
        var idArrOb = new Object[]{};
        if (!isEmpty(idArr)) {
            query += "WHERE ID_IN_REQ IN (" + nArg(idArr.length) + ")";
            idArrOb = idArr;
        }
        List<IncomingRequest> res = template.query(query, idArrOb, newArray(idArrOb.length, VARCHAR), (rs, i) -> {
            IncomingRequest in = new IncomingRequest(rs.getString("ID_IN_REQ"));
            in.setProtocol(rs.getString("VA_PRTCL"));
            in.setHost(rs.getString("VA_HST"));
            in.setPort(rs.getInt("CD_PRT"));
            in.setPath(rs.getString("VA_PTH"));
            in.setQuery(rs.getString("VA_QRY"));
            in.setMethod(rs.getString("VA_MTH"));
            in.setStatus(rs.getInt("CD_STT"));
            in.setSize(rs.getLong("VA_SZE"));
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

        var outReqMap = getOutcomingRequestListForInReq(idArr).stream().collect(groupingBy(ServerOutcomingRequest::getIdIncoming));
        var outQryMap = getOutcomingQueryListForInReq(idArr).stream().collect(groupingBy(ServerOutcomingQuery::getIdIncoming));

        for (IncomingRequest in : res) {
            if (outQryMap.containsKey(in.getId()))
                in.getQueries().addAll(outQryMap.get(in.getId()));
            if (outReqMap.containsKey(in.getId()))
                in.getRequests().addAll(outReqMap.get(in.getId()));
        }
        return res;

    }

    public List<ServerOutcomingRequest> getOutcomingRequestListForInReq(String... idArr) {

        var query = "SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ ";
        var idArrOb = new Object[]{};
        if (!isEmpty(idArr)) {
            query += "WHERE CD_IN_REQ IN (" + nArg(idArr.length) + ")";
            idArrOb = idArr;
        }

        return template.query(query, idArrOb, newArray(idArrOb.length, VARCHAR), (rs, i) -> {
            ServerOutcomingRequest out = new ServerOutcomingRequest(rs.getString("ID_OUT_REQ"), rs.getString("CD_IN_REQ"));
            out.setProtocol(rs.getString("VA_PRTCL"));
            out.setHost(rs.getString("VA_HST"));
            out.setPort(rs.getInt("CD_PRT"));
            out.setPath(rs.getString("VA_PTH"));
            out.setQuery(rs.getString("VA_QRY"));
            out.setMethod(rs.getString("VA_MTH"));
            out.setStatus(rs.getInt("CD_STT"));
            out.setSize(rs.getLong("VA_SZE"));
            out.setStart(rs.getTimestamp("DH_DBT").toInstant());
            out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            out.setThread(rs.getString("VA_THRED"));
            return out;
        });
    }

    public List<ServerOutcomingQuery> getOutcomingQueryListForInReq(String... idArr) {
        var query = "SELECT ID_OUT_QRY,VA_URL,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_QRY ";
        var idArrOb = new Object[]{};
        if (!isEmpty(idArr)) {
            idArrOb = idArr;
            query += "WHERE CD_IN_REQ IN (" + nArg(idArr.length) + ")";
        }
        List<Long> idQryArr = new LinkedList<>();
        List<ServerOutcomingQuery> outList = template.query(query, idArrOb, newArray(idArrOb.length, VARCHAR), (rs, i) -> { //compile pas ambigue !!
            ServerOutcomingQuery out = new ServerOutcomingQuery(rs.getString("CD_IN_REQ"), rs.getLong("ID_OUT_QRY"));
            out.setUrl(rs.getString("VA_URL"));
            out.setStart(rs.getTimestamp("DH_DBT").toInstant());
            out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
            out.setThread(rs.getString("VA_THRED"));
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
        if (!isEmpty(queries)) {
            query += "WHERE CD_OUT_QRY IN (" + nArg(queries.size()) + ")";
        }
        return template.query(query, queries.toArray(), newArray(queries.size(), BIGINT), (rs, i) ->
                new ServerDatabaAction(
                        rs.getLong("CD_OUT_QRY"),
                        Action.valueOf(rs.getString("VA_TYP")),
                        rs.getTimestamp("DH_DBT").toInstant(),
                        rs.getTimestamp("DH_FIN").toInstant(),
                        rs.getBoolean("VA_FAIL")));
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

        public ServerOutcomingQuery(String idIncoming, Long idOutQry) {
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