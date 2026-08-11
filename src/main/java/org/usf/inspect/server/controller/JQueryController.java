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

/**
 * REST controller exposing generic, dynamic jQuery-based read endpoints over the various
 * inspect trace views (sessions, requests, exceptions, instances, resources and logs).
 */
@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class JQueryController {

    /**
     * Executes a dynamic query against the {@code main_session} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "session/main", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getMainSession(
            @QueryRequest(view = "main_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code rest_session} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "session/rest", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getRestSession(
    		@QueryRequest(view = "rest_session", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code rest_request} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "request/rest", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getRestRequest(
    		@QueryRequest(view = "rest_request",defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code database_request} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "request/database", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getDatabaseRequest(
            @QueryRequest(view = "database_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code ftp_request} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "request/ftp", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getFtpRequest(
            @QueryRequest(view = "ftp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code smtp_request} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "request/smtp", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getSmtpRequest(
            @QueryRequest(view= "smtp_request", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code ldap_request} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "request/ldap", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getLdapRequest(
            @QueryRequest(view = "ldap_request", defaultColumns = "count") QueryComposer query){
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code exception} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "exception", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getException(
            @QueryRequest(view = "exception", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code user_action} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "user/action", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getUserAction(
            @QueryRequest(view = "user_action", defaultColumns = "count") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code instance} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "instance", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getInstance(
            @QueryRequest(view = "instance") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code instance_trace} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "instance/trace", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getInstanceTrace(
            @QueryRequest(view = "instance_trace") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code resource_usage} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "resource/machine", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getResourceMachine(
            @QueryRequest(view = "resource_usage") QueryComposer query) {
        return INSPECT.execute(query);
    }

    /**
     * Executes a dynamic query against the {@code log_entry} view.
     *
     * @param query the dynamic query request built from the incoming HTTP request
     * @return the list of matching rows
     */
    @GetMapping(value = "log/entry", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> getLogEntry(
            @QueryRequest(view = "log_entry") QueryComposer query) {
        return INSPECT.execute(query);
    }
}
