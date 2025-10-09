package org.usf.inspect.server.controller;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.core.BasicDispatchState.DISABLE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.service.DatabaseDispatcherService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "cache", produces = APPLICATION_JSON_VALUE)
public class CacheController {

    private final DatabaseDispatcherService service;
    private final RestTemplate template;
    private final ObjectMapper mapper;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

	private String host = null;

	public CacheController(ObjectMapper mapper, DatabaseDispatcherService service, RestTemplateBuilder builder) {
		this.service = service;
		this.mapper = mapper;
		this.template = builder //load interceptors
				.messageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
	}

    @PostMapping("{env}/import")
    public int importTraceable(@PathVariable String env) {
    	if(activeProfile.equals(env) && host != null) {
	    	template.postForLocation(host + "/v4/trace/state/"+ DISABLE, null); //stop adding session first on remote server
	        var arr = template.getForObject(host + "/v4/trace/queue", EventTrace[].class); //import sessions from remote server cache
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

    @PostMapping("{env}/import/file")
    public int importTraceableFromFile(@PathVariable String env, @RequestParam("file") MultipartFile file) {
        if(!activeProfile.equals(env)) {
            throw new IllegalArgumentException(String.format("mismatch env (actual : %s, expected : %s)", activeProfile, env));
        }

        if(file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne peut pas être vide"); //fr_en !?
        }

        try {
            var arr = mapper.readValue(file.getInputStream(), EventTrace[].class);
            if(nonNull(arr) && arr.length > 0) {
                var cnt = service.addTraces(asList(arr)); //save sessions on database
                if(!cnt.isEmpty()) {
                    log.warn("{} sessions was imported from file, but {} sessions was not saved", arr.length, cnt);
                }
                return arr.length;
            }
            return 0;
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier", e);
            throw new RuntimeException("Erreur lors de la lecture du fichier: " + e.getMessage(), e);
        }
    }
}