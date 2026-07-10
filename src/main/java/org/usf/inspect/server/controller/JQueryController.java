package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.QueryTemplate;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class JQueryController {

    @GetMapping("session/main")
    @QueryTemplate(dataset = "main_session", select= "count") 
    public Object fetchMainSession(MvcRequest mvc, HttpServletResponse res) {
        return mvc.execute(res);
    }
    
    @GetMapping("session/rest")
    @QueryTemplate(dataset = "rest_session", select= "count") 
    public Object fetchRestSession(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/rest")
    @QueryTemplate(dataset = "rest_request", select= "count") 
    public Object fetchRestRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/database")
    @QueryTemplate(dataset = "database_request", select= "count") 
    public Object getDatabaseRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ftp")
    @QueryTemplate(dataset = "ftp_request", select= "count") 
    public Object getFtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/smtp")
    @QueryTemplate(dataset = "smtp_request", select= "count") 
    public Object getSmtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ldap")
    @QueryTemplate(dataset = "ldap_request", select= "count") 
    public Object getLdapRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("exception")
    @QueryTemplate(dataset = "exception", select= "count") 
    public Object getException(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("user/action")
    @QueryTemplate(dataset = "user_action", select= "count") 
    public Object getUserAction(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance")
    @QueryTemplate(dataset = "instance", select= "count") 
    public Object getInstance(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance/trace")
    @QueryTemplate(dataset = "instance_trace", select= "count") 
    public Object getInstanceTrace(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("resource/machine")
    @QueryTemplate(dataset = "resource_usage", select= "count") 
    public Object getResourceMachine(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("log/entry")
    @QueryTemplate(dataset = "log_entry", select= "count") 
    public Object getLogEntry(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
}
