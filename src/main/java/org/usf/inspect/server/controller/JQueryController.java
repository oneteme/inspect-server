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

    @GetMapping(value = "session/main", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getMainSession(
            @QueryRequest(view = "main_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "session/rest", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getRestSession(
    		@QueryRequest(view = "rest_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "request/rest", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getRestRequest(
    		@QueryRequest(view = "rest_request",defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "request/database", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getDatabaseRequest(
            @QueryRequest(view = "database_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "request/ftp", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getFtpRequest(
            @QueryRequest(view = "ftp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "request/smtp", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getSmtpRequest(
            @QueryRequest(view= "smtp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "request/ldap", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getLdapRequest(
            @QueryRequest(view = "ldap_request", defaultColumns = "count") QueryComposer query){
        return INSPECT.execute(query);
    }

    @GetMapping(value = "exception", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getException(
            @QueryRequest(view = "exception", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "user/action", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getUserAction(
            @QueryRequest(view = "user_action", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "instance", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getInstance(
            @QueryRequest(view = "instance") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "instance/trace", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getInstanceTrace(
            @QueryRequest(view = "instance_trace") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "resource/machine", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getResourceMachine(
            @QueryRequest(view = "resource_usage") QueryComposer query) {
        return INSPECT.execute(query);
    }

    @GetMapping(value = "log/entry", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getLogEntry(
            @QueryRequest(view = "log_entry") QueryComposer query) {
        return INSPECT.execute(query);
    }
}
