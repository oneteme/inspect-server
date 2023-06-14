package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;


import org.springframework.web.bind.annotation.*;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingRequest;

import java.util.List;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.Utils.requireSingle;

@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {
    private final RequestDao dao;

    @PutMapping("incoming/request")
    public IncomingRequest saveRequest(@RequestBody IncomingRequest req) {
        return dao.addIncomingRequest(req);
    }

    @GetMapping("incoming/request")
    public List<IncomingRequest> getIncomingRequestByIds(@RequestParam(defaultValue = "true",name = "lazy") boolean lazy,@RequestParam(required = false,name="id") String[] id) { // without tree
        return dao.getIncomingRequestById(lazy,id);
    }

    @GetMapping("incoming/request/{id}")
    public IncomingRequest getIncomingRequestById(@PathVariable String id) { // without tree
        return requireSingle(dao.getIncomingRequestById(true, id));
    }


    @GetMapping("incoming/request/{id}/out")
    public OutcomingRequest getOutcomingRequestById(@PathVariable String id){
        return dao.getOutcomingRequestById(id);
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public IncomingRequest getIncomingRequestTreeById(@PathVariable String id) {
        return requireSingle(dao.getIncomingRequestById(true,id)); //change query
    }

}
