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
            @RequestQueryParam(name = "INCOMING_REQUEST_TABLE", defaultColumns = "STATUS") RequestQueryBuilder query) {
        return usingSpringJdbc(query);
    }
    
    @GetMapping(value="incoming/request/csv", produces = TEXT_PLAIN_VALUE)
    public void csv(
    		HttpServletResponse resp,
            @RequestQueryParam(name = "INCOMING_REQUEST_TABLE", defaultColumns = "STATUS") RequestQueryBuilder query) {
        try {
			query.build().toCsv(template.getDataSource(), resp.getWriter());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    @GetMapping(value="incoming/request/ascii", produces = TEXT_PLAIN_VALUE)
    public void ascii(
    		HttpServletResponse resp,
            @RequestQueryParam(name = "INCOMING_REQUEST_TABLE", defaultColumns = "STATUS") RequestQueryBuilder query) {
        try {
			query.build().toAscii(template.getDataSource(), resp.getWriter());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    @GetMapping(value="incoming/request/debug", produces = TEXT_PLAIN_VALUE)
    public void debug(
    		HttpServletResponse resp,
            @RequestQueryParam(name = "INCOMING_REQUEST_TABLE", defaultColumns = "STATUS") RequestQueryBuilder query) {

    	query.build().logResult(template.getDataSource());
    }
    
    @GetMapping(value="incoming/request/chart", produces = TEXT_HTML_VALUE)
    public void table(
    		HttpServletResponse resp,
    		@RequestParam(value="chart.type", defaultValue = "table") String chart,
            @RequestQueryParam(name = "INCOMING_REQUEST_TABLE", defaultColumns = "STATUS", ignoreParameters = "chart.type") RequestQueryBuilder query) {

    	try {
			query.build().toChart(template.getDataSource(), resp.getWriter(), chart);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    private List<DynamicModel> usingSpringJdbc(RequestQueryBuilder req) {
    	var query = req.build();
        return template.query(query.getQuery(), query.defaultMapper()::map, query.getParams());
    }
}
