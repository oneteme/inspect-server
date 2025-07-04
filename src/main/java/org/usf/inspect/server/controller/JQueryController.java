package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.QueryRequest;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class JQueryController {

    @GetMapping("session/main")
    public List<DynamicModel> getMainSession(
            @QueryRequest(view = "main_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("session/rest")
    public List<DynamicModel> getRestSession(
    		@QueryRequest(view = "rest_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("request/rest")
    public List<DynamicModel> getRestRequest(
    		@QueryRequest(view = "rest_request",defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("request/database")
    public List<DynamicModel> getDatabaseRequest(
            @QueryRequest(view = "database_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("request/ftp")
    public List<DynamicModel> getFtpRequest(
            @QueryRequest(view = "ftp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("request/smtp")
    public List<DynamicModel> getSmtpRequest(
            @QueryRequest(view= "smtp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("request/ldap")
    public List<DynamicModel> getLdapRequest(
            @QueryRequest(view = "ldap_request", defaultColumns = "count") QueryComposer query){
        return INSPECT.execute(query);
    }

    @GetMapping("exception")
    public List<DynamicModel> getException(
            @QueryRequest(view = "exception", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("user/action")
    public List<DynamicModel> getUserAction(
            @QueryRequest(view = "user_action", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping("instance")
    public List<DynamicModel> getInstance(
            @QueryRequest(view = "instance") QueryComposer query) {
        return INSPECT.execute(query);
    }
}
