package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.model.InstanceSession;
import org.usf.inspect.server.service.SessionQueueService;
import org.usf.traceapi.core.Session;

import java.util.Collection;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CacheController {
	
    private final SessionQueueService queue;

    @GetMapping
    public Collection<InstanceSession> getCache(){
    	return queue.waitList();
    }
}