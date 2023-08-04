package org.usf.trace.api.server;

import static java.util.Arrays.asList;
import static org.usf.jquery.web.JQueryContext.register;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.trace.api.server.config.TraceApiColumn;
import org.usf.trace.api.server.config.TraceApiTable;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebmvcConfig implements WebMvcConfigurer {

    private final DataSource ds;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        register(asList(TraceApiTable.values()),
        		asList(TraceApiColumn.values()))
        .bind(ds)
        .fetch(); //refresh first ?
        resolvers.add(new CommonRequestQueryResolver());
    }
}
