package org.usf.inspect.server.controller;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.core.DispatchState.DISABLE;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.server.model.ServerSession;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;

import com.fasterxml.jackson.databind.ObjectMapper;

@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
public class CacheController {

    private final RequestService service;
    private final SessionQueueService queue;
    private final RestTemplate template;
    
    @Value("${spring.profiles.active}")
    private String activeProfile;

	public CacheController(ObjectMapper mapper, RequestService service, SessionQueueService queue) {
		this.service = service;
		this.queue = queue;
		this.template = new RestTemplateBuilder()
				.messageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
	}

    @GetMapping
    public Collection<ServerSession> getCache(){
    	return queue.waitList();
    }

    @PatchMapping("state/{state}")
    public void updateState(@PathVariable DispatchState state){
        queue.enableSave(state);
    }

    @PostMapping("{env}/import")
    public int importSession(@PathVariable String env, @RequestParam String host) {
    	if(activeProfile.equals(env)) {
	    	template.patchForObject(host + "/state/"+ DISABLE, null, Void.class); //stop adding session first on remote server
	        var arr = template.getForObject(host + "/cache", ServerSession[].class); //import sessions from remote server cache
	        if(nonNull(arr) && arr.length > 0) {
	            service.addSessions(asList(arr)); //save sessions on database (local.env == remote.env)
	            return arr.length;
	        }
	        return 0;
    	}
    	throw new IllegalArgumentException("mismatch env " + env);
    }
}