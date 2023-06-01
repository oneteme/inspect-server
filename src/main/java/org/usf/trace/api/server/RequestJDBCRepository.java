package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingQuery;
import org.usf.traceapi.core.OutcomingRequest;
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
        jdbcTemplate.update("INSERT INTO E_IN_REQ VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                req.getId(),
                req.getUrl(),
                req.getMethod(),
                req.getStatus(),
                req.getSize(),
                req.getStart(),
                req.getEnd(),
                req.getContentType(),
                req.getApplication(),
                req.getEndpoint(),
                req.getResource(),
                req.getClient(),
                req.getQuery());
        addOucomingRequest(req.getRequests(),req.getId());
        addOutcomingQueries(req.getQueries(), req.getId());
        return req;
    }

    private void addOucomingRequest(Collection<OutcomingRequest> reqList, String incomingRequestId){
        this.jdbcTemplate.batchUpdate("INSERT INTO E_OUT_REQ  VALUES (?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o)-> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getUrl());
            ps.setString(3, o.getMethod());
            ps.setInt   (4, o.getStatus());
            ps.setLong(5,o.getSize());
            ps.setTimestamp(6,Timestamp.from( o.getStart()));
            ps.setTimestamp(7, Timestamp.from(o.getEnd()));
            ps.setString(8, incomingRequestId);
        });
    }

    private void addOutcomingQueries(Collection<OutcomingQuery> qryList,String incomingRequestId){
        final Integer maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_OUT_QRY", Integer.class);
        var map = new LinkedHashMap<OutcomingQuery, Integer>();
        AtomicInteger inc = new AtomicInteger(maxId);
        jdbcTemplate.batchUpdate("INSERT INTO E_OUT_QRY VALUES (?,?,?,?,?)", qryList, qryList.size(), (ps, o)-> {
            ps.setInt(1, inc.incrementAndGet());
            ps.setString(2,o.getUrl());
            ps.setTimestamp(3, Timestamp.from(o.getStart()));
            ps.setTimestamp(4, Timestamp.from(o.getEnd()));
            ps.setString(5, incomingRequestId);
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
}
