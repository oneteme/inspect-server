package org.usf.trace.api.server;

import static java.sql.Timestamp.from;
import static java.util.stream.Collectors.toList;
import static org.usf.trace.api.server.Utils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    public IncomingRequest  addIncomingRequest(IncomingRequest req){
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
        addOucomingRequest(req.getRequests(),req.getId());
        addOutcomingQueries(req.getQueries(), req.getId());
        return req;
    }

    private void addOucomingRequest(Collection<OutcomingRequest> reqList, String incomingRequestId){
        template.batchUpdate("INSERT INTO E_OUT_REQ (ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'"+incomingRequestId+"')", reqList, reqList.size(), (ps, o)-> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getProtocol());
            ps.setString(3, o.getHost());
            ps.setInt(4, o.getPort());
            ps.setString(5, o.getPath());
            ps.setString(6, o.getQuery());
            ps.setString(7, o.getMethod());
            ps.setInt   (8, o.getStatus());
            ps.setLong(9,o.getSize());
            ps.setTimestamp(10, from( o.getStart()));
            ps.setTimestamp(11, from(o.getEnd()));
            ps.setString(12, o.getThread());
        });
    }

    private void addOutcomingQueries(Collection<OutcomingQuery> qryList,String incomingRequestId){
        final Integer maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Integer.class);
        var map = new LinkedHashMap<OutcomingQuery, Integer>();
        AtomicInteger inc = new AtomicInteger(maxId);
        template.batchUpdate("INSERT INTO E_OUT_QRY (ID_OUT_QRY,VA_URL,DH_DBT,DH_FIN,CD_IN_REQ) VALUES  (?,?,?,?,'"+incomingRequestId+"')", qryList, qryList.size(), (ps, o)-> {
            ps.setInt(1, inc.incrementAndGet());
            ps.setString(2,o.getUrl());
            ps.setTimestamp(3, from(o.getStart()));
            ps.setTimestamp(4, from(o.getEnd()));
            map.put(o, inc.get());
        });
        addDatabaseAction(map);
    }

    private void addDatabaseAction(Map<OutcomingQuery, Integer> map){
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY) VALUES (?,?,?,?,?)", map.entrySet().stream().flatMap(e ->
                e.getKey().getActions().stream().map(da ->
                        new Object[]{da.getType().toString(), from(da.getStart()), from(da.getEnd()), da.isFailed(), e.getValue()})
        ).collect(toList()));
    }

    public List<IncomingRequest> getIncomingRequestById(String... idArr){
        var query = "SELECT ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP FROM E_IN_REQ ";
        if(isEmpty(idArr)){
        	query += "WHERE ID_IN_REQ IN (?)";
        }
        return template.query(query, idArr, rs -> {
            List<IncomingRequest> res = new ArrayList<>();
            while(rs.next()){
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
                in.getRequests().addAll(getOutcomingRequestListForInReq(rs.getString("ID_IN_REQ"))); //TODO must be map by IncomingRequest id
                in.getQueries().addAll(getOutcomingQueryListForInReq(rs.getString("ID_IN_REQ"))); //TODO must be map by IncomingRequest id
                res.add(in);
            }
            return res;
        });
    }

    public Collection<OutcomingRequest> getOutcomingRequestListForInReq(String incomingRequestId){
        return template.query("SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ WHERE CD_IN_REQ = ?",
            new Object[]{incomingRequestId},resultSet -> {
                List<OutcomingRequest> res = new ArrayList<>();
                while(resultSet.next()){
                    OutcomingRequest out = new OutcomingRequest(resultSet.getString("ID_OUT_REQ"));
                    out.setProtocol(resultSet.getString("VA_PRTCL"));
                    out.setHost(resultSet.getString("VA_HST"));
                    out.setPort(resultSet.getInt("CD_PRT"));
                    out.setPath(resultSet.getString("VA_PTH"));
                    out.setQuery(resultSet.getString("VA_QRY"));
                    out.setMethod(resultSet.getString("VA_MTH"));
                    out.setStatus(resultSet.getInt("CD_STT"));
                    out.setSize(resultSet.getLong("VA_SZE"));
                    out.setStart(resultSet.getTimestamp("DH_DBT").toInstant());
                    out.setEnd(resultSet.getTimestamp("DH_FIN").toInstant());
                    out.setThread(resultSet.getString("VA_THRED"));
                    res.add(out);
                }
                return res;
            });
    }

    public Collection<OutcomingQuery> getOutcomingQueryListForInReq(String incomingRequestId ){
        return template.query("SELECT ID_OUT_QRY,VA_URL,DH_DBT,DH_FIN,CD_IN_REQ FROM E_OUT_QRY WHERE CD_IN_REQ = ?",
            new Object[]{incomingRequestId},rs -> {
                List<OutcomingQuery> outcomingQueryList = new ArrayList<>();
                while(rs.next()){
                    OutcomingQuery out = new OutcomingQuery();
                    out.setUrl(rs.getString("VA_URL"));
                    out.setStart(rs.getTimestamp("DH_DBT").toInstant());
                    out.setEnd(rs.getTimestamp("DH_FIN").toInstant());
                    out.getActions().addAll(getDatabaseActionListForOutReq(rs.getLong("ID_OUT_QRY")));
                    outcomingQueryList.add(out);
                }
                return outcomingQueryList;
            });
    }

    public List<DatabaseAction> getDatabaseActionListForOutReq(Long idOutcomingQuery ){
        return template.query("SELECT VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY FROM E_DB_ACT WHERE CD_OUT_QRY = ?",
	        new Object[]{idOutcomingQuery},rs -> {
	            List<DatabaseAction> databaseActionList = new ArrayList<>();
	            while(rs.next()){
	                DatabaseAction dbAction = new DatabaseAction(
	                        Action.valueOf(rs.getString("VA_TYP")),
	                        rs.getTimestamp("DH_DBT").toInstant(),
	                        rs.getTimestamp("DH_FIN").toInstant(),
	                        rs.getBoolean("VA_FAIL"));
	                databaseActionList.add(dbAction);
	            }
	            return databaseActionList;
	        });
    }

}