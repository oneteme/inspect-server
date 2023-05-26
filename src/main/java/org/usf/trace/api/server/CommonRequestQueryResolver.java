package org.usf.trace.api.server;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.usf.jquery.core.RequestQuery;
import org.usf.jquery.web.RequestQueryParam;
import org.usf.jquery.web.RequestQueryParamResolver;

public class CommonRequestQueryResolver implements HandlerMethodArgumentResolver {

    private final RequestQueryParamResolver resolver = new RequestQueryParamResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestQuery.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(RequestQueryParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        var crp = parameter.getParameterAnnotation(RequestQueryParam.class);
        return resolver.requestQuery(crp, webRequest.getParameterMap());
    }
}

