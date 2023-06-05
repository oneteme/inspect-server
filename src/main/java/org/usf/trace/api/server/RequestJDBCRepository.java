package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.traceapi.core.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RequestJDBCRepository {
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public IncomingRequest  addIncomingRequest(IncomingRequest req){
        jdbcTemplate.update("INSERT INTO E_IN_REQ VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
        this.jdbcTemplate.batchUpdate("INSERT INTO E_OUT_REQ  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,"+incomingRequestId+")", reqList, reqList.size(), (ps, o)-> {
            ps.setString(1, o.getId());
            ps.setString(2,o.getProtocol());
            ps.setString(3,o.getHost());
            ps.setInt(4,o.getPort());
            ps.setString(5,o.getPath());
            ps.setString(6,o.getQuery());
            ps.setString(7, o.getMethod());
            ps.setInt   (8, o.getStatus());
            ps.setLong(9,o.getSize());
            ps.setTimestamp(10,Timestamp.from( o.getStart()));
            ps.setTimestamp(11, Timestamp.from(o.getEnd()));
            ps.setString(12, o.getThread());
        });
    }

    private void addOutcomingQueries(Collection<OutcomingQuery> qryList,String incomingRequestId){
        final Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Integer.class);
        var map = new LinkedHashMap<OutcomingQuery, Integer>();
        AtomicInteger inc = new AtomicInteger(maxId);
        jdbcTemplate.batchUpdate("INSERT INTO E_OUT_QRY VALUES (?,?,?,?,"+incomingRequestId+")", qryList, qryList.size(), (ps, o)-> {
            ps.setInt(1, inc.incrementAndGet());
            ps.setString(2,o.getUrl());
            ps.setTimestamp(3, Timestamp.from(o.getStart()));
            ps.setTimestamp(4, Timestamp.from(o.getEnd()));
            map.put(o, inc.get());
        });
        addDatabaseAction(map);
    }

    private void addDatabaseAction(Map<OutcomingQuery, Integer> map){
        jdbcTemplate.batchUpdate("INSERT INTO E_DB_ACT VALUES (?,?,?,?,?)", map.entrySet().stream().flatMap(e ->
                e.getKey().getActions().stream().map(da ->
                        new Object[]{da.getType(), Timestamp.from(da.getStart()), Timestamp.from(da.getEnd()), da.isFailed(), e.getValue()})
        ).collect(Collectors.toList()));
    }

    public List<IncomingRequest> getIncomingRequestById(String[] idArr){
        String whereCLause = "";
        if(idArr !=null){
             whereCLause = "where ID_IN_REQ IN (?)";
        }
        return jdbcTemplate.query("SELECT ID_IN_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,VA_CNT_TYP,VA_ACT,VA_RSC,VA_CLI,VA_GRP FROM E_IN_REQ "+whereCLause,
                    idArr,rs -> {
                        List<IncomingRequest> inReqList = new ArrayList<>();

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
                            in.getRequests().addAll(getOutcomingRequestListForInReq(rs.getString("ID_IN_REQ")));
                            in.getQueries().addAll(getOutcomingQueryListForInReq(rs.getString("ID_IN_REQ")));
                            inReqList.add(in);
                        }
                        return  inReqList;
                    }
                );
    }

    public Collection<OutcomingRequest> getOutcomingRequestListForInReq(String incomingRequestId){
        return jdbcTemplate.query("SELECT ID_OUT_REQ,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_MTH,CD_STT,VA_SZE,DH_DBT,DH_FIN,VA_THRED,CD_IN_REQ FROM E_OUT_REQ WHERE CD_IN_REQ = ?",
                new Object[]{incomingRequestId},resultSet -> {
                    List<OutcomingRequest> outcomingRequestList = new ArrayList<>();
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
                        outcomingRequestList.add(out);
                    }
                    return outcomingRequestList;
                });
    }

    public Collection<OutcomingQuery> getOutcomingQueryListForInReq(String incomingRequestId ){
        return jdbcTemplate.query("SELECT ID_OUT_QRY,VA_URL,DH_DBT,DH_FIN,CD_IN_REQ FROM E_OUT_QRY WHERE CD_IN_REQ = ?",
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
        return jdbcTemplate.query("SELECT VA_TYP,DH_DBT,DH_FIN,VA_FAIL,CD_OUT_QRY FROM E_DB_ACT WHERE CD_OUT_QRY = ?",
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


