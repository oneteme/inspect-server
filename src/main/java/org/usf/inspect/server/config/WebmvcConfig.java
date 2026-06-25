package org.usf.inspect.server.config;

import static org.usf.jquery.web.proxy.StoreManager.getInstance;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.usf.inspect.server.repo.InspectStore;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebmvcConfig implements WebMvcConfigurer {

    private final DataSource ds;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    	getInstance().register(InspectStore.class, ds);
        resolvers.add(new CommonRequestQueryResolver());
        resolvers.add(new CommonRequestQueryFilterResolver());
    }
}
