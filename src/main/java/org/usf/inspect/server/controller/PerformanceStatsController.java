package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.web.RequestQueryParam;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "stat", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PerformanceStatsController {

    private final JdbcTemplate template;

    @GetMapping("apisession")
    public List<DynamicModel> request(
            @RequestQueryParam(name = "apisession", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("mainsession")
    public List<DynamicModel> session(
            @RequestQueryParam(name = "mainsession", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping("instance")
    public List<DynamicModel> environment(
            @RequestQueryParam(name = "instance") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    private List<DynamicModel> usingSpringJdbc(RequestQueryBuilder req) {
    	var query = req.build();
        return template.query(query.getQuery(), query.defaultMapper()::map, query.getParams());
    }
}
