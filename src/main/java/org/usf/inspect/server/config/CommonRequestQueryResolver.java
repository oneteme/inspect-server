package org.usf.inspect.server.config;

import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.QueryRequest;
import org.usf.jquery.web.QueryRequestResolver;

public class CommonRequestQueryResolver implements HandlerMethodArgumentResolver {

    private final QueryRequestResolver resolver = new QueryRequestResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return QueryComposer.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(QueryRequest.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var crp = Objects.requireNonNull(parameter.getParameterAnnotation(QueryRequest.class));
        return resolver.requestQuery(crp, webRequest.getParameterMap());
    }
}

