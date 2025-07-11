package org.usf.inspect.server.config;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.QueryRequestFilter;
import org.usf.jquery.web.QueryRequestFilterResolver;

import java.util.Objects;

public class CommonRequestQueryFilterResolver implements HandlerMethodArgumentResolver {

    private final QueryRequestFilterResolver resolver = new QueryRequestFilterResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return QueryComposer.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(QueryRequestFilter.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        var crp = parameter.getParameterAnnotation(QueryRequestFilter.class);
        return resolver.requestQueryCheck(
                Objects.requireNonNull(crp, "QueryRequestFilter annotation is required"),
                webRequest.getParameterMap()
        );
    }
}

