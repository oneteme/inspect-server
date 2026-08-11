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

/**
 * Resolves query composer arguments from HTTP request parameters.
 */
public class CommonRequestQueryResolver implements HandlerMethodArgumentResolver {

    private final QueryRequestResolver resolver = new QueryRequestResolver();

    /**
     * Determines whether the current method parameter can be resolved as a query composer.
     *
     * @param parameter the method parameter to inspect
     * @return {@code true} if the parameter is a supported query composer, otherwise {@code false}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return QueryComposer.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(QueryRequest.class);
    }

    /**
     * Resolves a query composer argument from the current web request.
     *
     * @param parameter the method parameter being resolved
     * @param mavContainer the model and view container for the current request
     * @param webRequest the current native web request
     * @param binderFactory the factory used to create data binders when needed
     * @return the resolved query composer argument
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var crp = Objects.requireNonNull(parameter.getParameterAnnotation(QueryRequest.class));
        return resolver.requestQuery(crp, webRequest.getParameterMap());
    }
}
