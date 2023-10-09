package org.usf.trace.api.server;


import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.traceapi.core.ApiRequest;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TreeController {

    private final RequestDao dao;

    @GetMapping("tree/request/{id}")
    public ApiRequest getTreebyId(@PathVariable String id){
        return dao.getTreebyId(id);
    }
}
