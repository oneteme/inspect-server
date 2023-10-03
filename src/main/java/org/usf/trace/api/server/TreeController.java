package org.usf.trace.api.server;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.usf.traceapi.core.IncomingRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.traceapi.core.RemoteTraceSender.TRACE_ENDPOINT;

@CrossOrigin
@RestController
@RequestMapping(value = TRACE_ENDPOINT, produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TreeController {

    private final RequestDao dao;

    @GetMapping("tree/request/{id}")
    public IncomingRequest getTreebyId(@PathVariable String id){
        return dao.getTreebyId(id);
    }
}
