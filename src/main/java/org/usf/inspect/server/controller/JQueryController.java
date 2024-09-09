package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.KeyValueMapper;
import org.usf.jquery.core.QueryBuilder;
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
            @RequestQueryParam(view = "main_session", defaultColumns = "count") QueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("session/rest")
    public List<DynamicModel> getRestSession(
            @RequestQueryParam(view = "rest_session", defaultColumns = "count") QueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("request/database")
    public List<DynamicModel> getDatabaseRequest(
            @RequestQueryParam(view = "database_request", defaultColumns = "count") QueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("exception")
    public List<DynamicModel> getException(
            @RequestQueryParam(view = "exception", defaultColumns = "count") QueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("instance")
    public List<DynamicModel> getInstance(
            @RequestQueryParam(view = "instance") QueryBuilder query) {
        return usingSpringJdbc(query);
    }

    private List<DynamicModel> usingSpringJdbc(QueryBuilder req) {
        var query = req.build();
        return template.query(query.getQuery(), new KeyValueMapper()::map, query.getArgs());
    }
}
