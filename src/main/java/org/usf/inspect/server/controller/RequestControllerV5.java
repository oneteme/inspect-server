package org.usf.inspect.server.controller;

import static java.util.UUID.fromString;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.repo.InspectStore;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.QueryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@Validated
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestControllerV5 {

//    private final RequestService requestService;
    private final ObjectMapper mapper;

    @GetMapping(value = "instance/{idInstance}", produces = APPLICATION_JSON_VALUE)
     @QueryTemplate(dataset = "instance",view = "instanceMapper", select = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,resource,configuration,id") 
       public Object fetchMainSession(MvcRequest mvc, HttpServletResponse res, @PathVariable String idInstance) {
    	var store = (InspectStore)mvc.getStore();
    	mvc.getComposer().criteria(store.instance().id().eq(fromString(idInstance))); //UUID
    	
    	return ok()
        		.cacheControl(maxAge(1, HOURS))
        		.body(mvc.execute());
    }
   }
