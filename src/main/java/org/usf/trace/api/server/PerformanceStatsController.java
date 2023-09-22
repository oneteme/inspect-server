package org.usf.trace.api.server;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping(value="incoming/request", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> stats(
            @RequestQueryParam(name = "request", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }
    
    @GetMapping(value="session", produces = APPLICATION_JSON_VALUE)
    public List<DynamicModel> session(
            @RequestQueryParam(name = "session", defaultColumns = "count") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }

    private List<DynamicModel> usingSpringJdbc(RequestQueryBuilder req) {
    	var query = req.build();
        return template.query(query.getQuery(), query.defaultMapper()::map, query.getParams());
    }
}
