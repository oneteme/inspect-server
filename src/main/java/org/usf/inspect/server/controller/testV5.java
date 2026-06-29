package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.RequestQuery;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class testV5 {

    @GetMapping("session/main")
    @RequestQuery(dataset = "main_session", fields = "count") 
    public Object fetchMainSession(MvcRequest mvc, HttpServletResponse res) {
        return mvc.execute(res);
    }
    
    @GetMapping("session/rest")
    @RequestQuery(dataset = "rest_session", fields = "count") 
    public Object fetchRestSession(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/rest")
    @RequestQuery(dataset = "rest_request", fields = "count") 
    public Object fetchRestRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/database")
    @RequestQuery(dataset = "database_request", fields = "count") 
    public Object getDatabaseRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ftp")
    @RequestQuery(dataset = "ftp_request", fields = "count") 
    public Object getFtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/smtp")
    @RequestQuery(dataset = "smtp_request", fields = "count") 
    public Object getSmtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ldap")
    @RequestQuery(dataset = "ldap_request", fields = "count") 
    public Object getLdapRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("exception")
    @RequestQuery(dataset = "exception", fields = "count") 
    public Object getException(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("user/action")
    @RequestQuery(dataset = "user_action", fields = "count") 
    public Object getUserAction(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance")
    @RequestQuery(dataset = "instance", fields = "count") 
    public Object getInstance(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance/trace")
    @RequestQuery(dataset = "instance_trace", fields = "count") 
    public Object getInstanceTrace(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("resource/machine")
    @RequestQuery(dataset = "resource_usage", fields = "count") 
    public Object getResourceMachine(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("log/entry")
    @RequestQuery(dataset = "log_entry", fields = "count") 
    public Object getLogEntry(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
}
