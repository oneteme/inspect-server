package org.usf.inspect.server.controller;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.core.BasicDispatchState.DISABLE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.service.TraceService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
public class CacheController {

    private final TraceService service;
    private final RestTemplate template;
    
    @Value("${spring.profiles.active:}")
    private String activeProfile;

	private String host = null;

	public CacheController(ObjectMapper mapper, TraceService service) {
		this.service = service;
		this.template = new RestTemplateBuilder()
				.messageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
	}

    @PostMapping("{env}/import")
    public int importTraceable(@PathVariable String env) {
    	if(activeProfile.equals(env) && host != null) {
	    	template.postForLocation(host + "/v4/trace/state/"+ DISABLE, null); //stop adding session first on remote server
	        var arr = template.getForObject(host + "/cache", EventTrace[].class); //import sessions from remote server cache
	        if(nonNull(arr) && arr.length > 0) {
	            var cnt = service.addTraces(asList(arr)); //save sessions on database (local.env == remote.env)
	            if(!cnt.isEmpty()) {
	            	log.warn("{} sessions was imported, but {} sessions was not saved", arr.length, cnt);
	            }
	            return arr.length;
	        }
	        return 0;
    	}
    	throw new IllegalArgumentException(String.format("mismatch env (actual : %s, expected : %s)", activeProfile, env));
    }
}