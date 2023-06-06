 package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.ParametredQuery;

import org.usf.traceapi.core.IncomingRequest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.Utils.requireSingle;

@CrossOrigin
@RestController
@RequestMapping(value="trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {

    private final JdbcTemplate template;

    private final RequestDao dao;

    /*@GetMapping("incoming/request")
    public List<DynamicModel> stats( 
            @RequestQueryParam(name="E_iN_REQ", defaultColumns = "STATUS") RequestQuery query){
        return query.execute(this::usingSpringJdbc);
    }*/

    private List<DynamicModel> usingSpringJdbc(ParametredQuery query) {
        return query.hasNoResult()
                ? emptyList()
                : template.query(query.getQuery(), query::mapRows, query.getParams());
    }

    @PutMapping("incoming/request")
    public IncomingRequest saveRequest(@RequestBody IncomingRequest req) {
        return dao.addIncomingRequest(req);
    }

    @GetMapping("incoming/request")
    public List<IncomingRequest> getIncomingRequestByIds(@RequestParam(required = false) String[] id){ // without tree
        return  dao.getIncomingRequestById(id);
    }
    
    @GetMapping("incoming/request/{id}")
    public IncomingRequest getIncomingRequestById(@PathVariable String id){ // without tree
        return requireSingle(dao.getIncomingRequestById(id));
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public IncomingRequest getIncomingRequestTreeById(@PathVariable String id){
        return requireSingle(dao.getIncomingRequestById(id)); //change query
    }
    
}
