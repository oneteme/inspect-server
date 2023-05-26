 package org.usf.trace.api.server;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.ParametredQuery;
import org.usf.jquery.core.RequestQuery;
import org.usf.jquery.web.RequestQueryParam;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@CrossOrigin
@RestController
@RequestMapping(value="trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ApiController {

    private final JdbcTemplate template;

    @GetMapping("request")
    public List<DynamicModel> stats( 
            @RequestQueryParam(name="AGREEMENTS", defaultColumns = "STATUS") RequestQuery query){
        return query.execute(this::usingSpringJdbc);
    }

    private List<DynamicModel> usingSpringJdbc(ParametredQuery query) {

        return query.hasNoResult()
                ? emptyList()
                : template.query(query.getQuery(), query::mapRows, query.getParams());
    }
}
