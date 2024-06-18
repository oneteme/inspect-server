package org.usf.trace.api.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.web.RequestQueryParam;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "stat")
@RequiredArgsConstructor
public class PerformanceStatsController {

    private final JdbcTemplate template;

    @GetMapping(value="apisession", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> request(
            @RequestQueryParam(name = "apisession", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping(value="mainsession", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> session(
            @RequestQueryParam(name = "mainsession", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    @GetMapping(value="environment", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> environment(
            @RequestQueryParam(name = "apisession", defaultColumns = "environement") RequestQueryBuilder query
    ) {
        return usingSpringJdbc(query.distinct());
    }

    private List<DynamicModel> usingSpringJdbc(RequestQueryBuilder req) {
    	var query = req.build();
        return template.query(query.getQuery(), query.defaultMapper()::map, query.getParams());
    }
}
