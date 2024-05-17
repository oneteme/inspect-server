package org.usf.trace.api.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.trace.api.server.service.SessionQueueService;
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
    public Collection<Session> getCache(){
    	return queue.waitList();
    }
}