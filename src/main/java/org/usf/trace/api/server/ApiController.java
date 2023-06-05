 package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.ParametredQuery;

import org.usf.traceapi.core.IncomingRequest;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin
@RestController
@RequestMapping(value="trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {



    JdbcTemplate template;

    @Autowired
    RequestJDBCRepository requestJDBCRepository;

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
        return requestJDBCRepository.addIncomingRequest(req);
    }

    @GetMapping("incoming/request")
    public List<IncomingRequest> getIncomingRequestByIds(@RequestParam(required = false) String[] id){ // without tree
        return  requestJDBCRepository.getIncomingRequestById(id);
    }
    @GetMapping("incoming/request/{id}")
    public IncomingRequest getIncomingRequestById(@PathVariable String id){ // without tree
        return  requestJDBCRepository.getIncomingRequestById(new String[]{id}).get(0); //to be changed
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public IncomingRequest getIncomingRequestTreeById(@PathVariable String id){ // without tree
        return  requestJDBCRepository.getIncomingRequestById(new String[]{id}).get(0);// to be changed
    }

}
