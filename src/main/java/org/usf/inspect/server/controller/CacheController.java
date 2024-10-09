package org.usf.inspect.server.controller;

import static java.time.Duration.ofSeconds;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.model.ServerSession;
import org.usf.inspect.server.service.SessionQueueService;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CacheController {

    private final ObjectMapper mapper;
    private final RequestDao dao;
    private final SessionQueueService queue;

    RestTemplate defaultRestTemplate() {
        var json = new MappingJackson2HttpMessageConverter(mapper);
        var plain = new StringHttpMessageConverter(); //for instanceID
        var rt = new RestTemplateBuilder().messageConverters(json, plain)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        return rt.build();
    }

    @GetMapping
    public Collection<ServerSession> getCache(){
    	return queue.waitList();
    }

    @PatchMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
        queue.enableSave(state);
    }

    @PostMapping()
    public int addSession(@RequestParam(name = "host") String host) {
        var rt = defaultRestTemplate();
        var ses = rt.getForObject(host + "/cache", ServerSession[].class);
        if(ses != null && ses.length > 0) {
            dao.saveSessions(Arrays.asList(ses));
            return ses.length;
        }
        return 0;
    }
}