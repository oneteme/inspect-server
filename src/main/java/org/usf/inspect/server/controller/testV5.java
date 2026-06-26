package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.QueryComposer;
//import org.usf.jquery.web.QueryRequest;
import org.usf.jquery.web.proxy.MvcRequest;
import org.usf.jquery.web.proxy.QueryRequest;
import org.usf.jquery.web.proxy.ViewRegistry;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class testV5 {

    @GetMapping("session/main")
    @QueryRequest(dataset = "main_session", fields = "count") 
    public Object fetchMainSession(MvcRequest mvc, HttpServletResponse res) {
        return mvc.execute(res);
    }
    
    @GetMapping("session/rest")
    @QueryRequest(dataset = "rest_session", fields = "count") 
    public Object fetchRestSession(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/rest")
    @QueryRequest(dataset = "rest_request", fields = "count") 
    public Object fetchRestRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/database")
    @QueryRequest(dataset = "database_request", fields = "count") 
    public Object getDatabaseRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ftp")
    @QueryRequest(dataset = "ftp_request", fields = "count") 
    public Object getFtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/smtp")
    @QueryRequest(dataset = "smtp_request", fields = "count") 
    public Object getSmtpRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("request/ldap")
    @QueryRequest(dataset = "ldap_request", fields = "count") 
    public Object getLdapRequest(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("exception")
    @QueryRequest(dataset = "exception", fields = "count") 
    public Object getException(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("user/action")
    @QueryRequest(dataset = "user_action", fields = "count") 
    public Object getUserAction(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance")
    @QueryRequest(dataset = "instance", fields = "count") 
    public Object getInstance(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("instance/trace")
    @QueryRequest(dataset = "instance_trace", fields = "count") 
    public Object getInstanceTrace(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("resource/machine")
    @QueryRequest(dataset = "resource_usage", fields = "count") 
    public Object getResourceMachine(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
    
    @GetMapping("log/entry")
    @QueryRequest(dataset = "log_entry", fields = "count") 
    public Object getLogEntry(MvcRequest mvc, HttpServletResponse res) {
    	return mvc.execute(res);
    }
}
