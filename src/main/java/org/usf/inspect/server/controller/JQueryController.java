package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.QueryGuard;
import org.usf.jquery.mvc.QueryTemplate;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class JQueryController {

    private static final int MAX_ROWS = 300000;

    @GetMapping("session/main")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "main_session", select= "count") 
    public Object fetchMainSession(MvcRequest mvc, HttpServletResponse res) {
        return mvc.execute(res);
    }
    
    @GetMapping("session/rest")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "rest_session", select= "count") 
    public Object fetchRestSession(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/rest")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "rest_request", select= "count") 
    public Object fetchRestRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/database")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "database_request", select= "count") 
    public Object getDatabaseRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ftp")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "ftp_request", select= "count") 
    public Object getFtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/smtp")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "smtp_request", select= "count") 
    public Object getSmtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ldap")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "ldap_request", select= "count") 
    public Object getLdapRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("exception")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "exception", select= "count") 
    public Object getException(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }

    @GetMapping("instance")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "instance", select= "count") 
    public Object getInstance(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance/trace")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "instance_trace", select= "count") 
    public Object getInstanceTrace(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("resource/machine")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "resource_usage", select= "count") 
    public Object getResourceMachine(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("log/entry")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "log_entry", select= "count") 
    public Object getLogEntry(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }

    @GetMapping("session/event")
    @QueryGuard(maxRows = MAX_ROWS)
    @QueryTemplate(dataset = "session_event", select= "count")
    public Object fetchSessionEvent(MvcRequest mvc, HttpServletResponse res) {
        return mvc.execute(res);
    }
}
