package org.usf.inspect.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.server.model.object.Session;
import org.usf.inspect.server.service.RequestService;
import org.usf.inspect.server.service.SessionQueueService;

import java.util.Collection;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.DispatchState.DISABLE;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
public class CacheController {

    private final RequestService service;
    private final SessionQueueService queue;
    private final RestTemplate template;
    
    @Value("${spring.profiles.active}")
    private String activeProfile;

	private String host = null;

	public CacheController(ObjectMapper mapper, RequestService service, SessionQueueService queue) {
		this.service = service;
		this.queue = queue;
		this.template = new RestTemplateBuilder()
				.messageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
	}

    @GetMapping
    public ResponseEntity<Collection<Session>> getCache(){
		return ok(queue.waitList());
    }

    @PostMapping("state/{state}")
    public ResponseEntity<Void> updateState(@PathVariable DispatchState state){
		queue.enableSave(state);
		return ok().build();
    }

    @PostMapping("{env}/import")
    public int importSession(@PathVariable String env) {
    	if(activeProfile.equals(env) && host != null) {
	    	template.postForLocation(host + "/cache/state/"+ DISABLE, null); //stop adding session first on remote server
	        var arr = template.getForObject(host + "/cache", Session[].class); //import sessions from remote server cache
	        if(nonNull(arr) && arr.length > 0) {
	            var cnt = service.addSessions(asList(arr)); //save sessions on database (local.env == remote.env)
	            if(cnt != arr.length) {
	            	log.warn("{} sessions was imported, but {} sessions was saved", arr.length, cnt);
	            }
	            return arr.length;
	        }
	        return 0;
    	}
    	throw new IllegalArgumentException(String.format("mismatch env (actual : %s, expected : %s)", activeProfile, env));
    }
}