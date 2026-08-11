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

/**
 * Resolves filtered query composer arguments from HTTP request parameters.
 */
public class CommonRequestQueryFilterResolver implements HandlerMethodArgumentResolver {

    private final QueryRequestFilterResolver resolver = new QueryRequestFilterResolver();

    /**
     * Determines whether the current method parameter can be resolved as a filtered query composer.
     *
     * @param parameter the method parameter to inspect
     * @return {@code true} if the parameter is a supported filtered query composer, otherwise {@code false}
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return QueryComposer.class.isAssignableFrom(parameter.getNestedParameterType())
                && parameter.hasParameterAnnotation(QueryRequestFilter.class);
    }

    /**
     * Resolves a filtered query composer argument from the current web request.
     *
     * @param parameter the method parameter being resolved
     * @param mavContainer the model and view container for the current request
     * @param webRequest the current native web request
     * @param binderFactory the factory used to create data binders when needed
     * @return the resolved filtered query composer argument
     * @throws Exception if argument resolution fails
     */
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
