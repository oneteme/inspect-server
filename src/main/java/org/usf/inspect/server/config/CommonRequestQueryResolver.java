package org.usf.inspect.server.config;

import java.util.Objects;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.web.RequestQueryParam;
import org.usf.jquery.web.RequestQueryParamResolver;

public class CommonRequestQueryResolver implements HandlerMethodArgumentResolver {

    private final RequestQueryParamResolver resolver = new RequestQueryParamResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestQueryBuilder.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(RequestQueryParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var crp = Objects.requireNonNull(parameter.getParameterAnnotation(RequestQueryParam.class));
        return resolver.requestQuery(crp, webRequest.getParameterMap());
    }
}

