package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collection;

import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.server.model.ServerSession;
import org.usf.inspect.server.service.SessionQueueService;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CacheController {
	
    private final SessionQueueService queue;

    @GetMapping
    public Collection<ServerSession> getCache(){
    	return queue.waitList();
    }

    @PatchMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
        queue.enableSave(state);
    }
}