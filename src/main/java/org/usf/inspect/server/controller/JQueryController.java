package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.web.RequestQueryParam;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin
@RestController
@RequestMapping(value = "jquery", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class JQueryController {
    private final JdbcTemplate template;

    @GetMapping("session/main")
    public List<DynamicModel> getMainSession(
            @RequestQueryParam(name = "main_session", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("session/rest")
    public List<DynamicModel> getRestSession(
            @RequestQueryParam(name = "rest_session", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("request/database")
    public List<DynamicModel> getDatabaseRequest(
            @RequestQueryParam(name = "database_request", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("exception")
    public List<DynamicModel> getException(
            @RequestQueryParam(name = "exception", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("instance")
    public List<DynamicModel> getInstance(
            @RequestQueryParam(name = "instance") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    private List<DynamicModel> usingSpringJdbc(RequestQueryBuilder req) {
        var query = req.build();
        return template.query(query.getQuery(), query.defaultMapper()::map, query.getParams());
    }
}
