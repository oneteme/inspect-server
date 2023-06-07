package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;


import org.springframework.web.bind.annotation.*;
import org.usf.traceapi.core.IncomingRequest;
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
    public List<IncomingRequest> getIncomingRequestByIds(@RequestParam(required = false) String[] id) { // without tree
        return dao.getIncomingRequestById(id);
    }

    @GetMapping("incoming/request/{id}")
    public IncomingRequest getIncomingRequestById(@PathVariable String id) { // without tree
        return requireSingle(dao.getIncomingRequestById(id));
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public IncomingRequest getIncomingRequestTreeById(@PathVariable String id) {
        return requireSingle(dao.getIncomingRequestById(id)); //change query
    }

}
