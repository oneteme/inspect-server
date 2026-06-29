package org.usf.inspect.server.config;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Stream.concat;
import static org.usf.jquery.core.Utils.isEmpty;
import static org.usf.jquery.mvc.Parameters.CRITERIA_OPR;
import static org.usf.jquery.mvc.Parameters.SELECT_PARAM;
import static org.usf.jquery.mvc.Parameters.VIEW_PARAM;
import static org.usf.jquery.mvc.RestrictedStore.restrict;
import static org.usf.jquery.mvc.StoreManager.getInstance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.QueryInterpreter;
import org.usf.jquery.mvc.RequestQuery;
import org.usf.jquery.mvc.Restriction;
import org.usf.jquery.mvc.StoreResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonRequestQueryResolver implements HandlerMethodArgumentResolver, QueryInterpreter {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if(parameter.hasMethodAnnotation(RequestQuery.class)) {
			return parameter.getNestedParameterType() == MvcRequest.class;
		}
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		var ann = parameter.getMethodAnnotation(RequestQuery.class);
		if(nonNull(ann)) {
			if(parameter.getNestedParameterType() == MvcRequest.class) {
				return cacheAttribute(webRequest, MvcRequest.class, 
						()-> resolveQueryComposer(ann, parameter, webRequest));
			}
			throw new IllegalStateException("unsupported parameter type: " + parameter.getNestedParameterType());
		}
		throw new IllegalStateException("missing @QueryRequest annotation");
	}

	MvcRequest resolveQueryComposer(RequestQuery ann, MethodParameter parameter, NativeWebRequest webRequest) {
		var str = resolveStore(ann, parameter);
		var map = new LinkedHashMap<>(webRequest.getParameterMap()); //modifiable map + preserve order
		if(!isEmpty(ann.ignore())) {
			for(var k : ann.ignore()) {
				if(map.containsKey(k)) {
					log.debug("ignoring parameter '{}' as specified in @QueryRequest", k);
					map.remove(k);
				}
			}
		}
		resolveParameterCompatibility(map);
		map.computeIfAbsent(VIEW_PARAM, k-> new String[] {ann.view()});	
		map.computeIfAbsent(SELECT_PARAM, k-> ann.fields());	
		return parseQuery(str, ann.dataset(), map);
	}

	StoreResource resolveStore(RequestQuery ann, MethodParameter parameter) {
		var store = ann.store() == StoreResource.class 
				? getInstance().getDefaultStore() 
				: getInstance().getStore(ann.store());
		var rst = parameter.getMethodAnnotation(Restriction.class);
		return nonNull(rst) 
				? restrict(store, rst.maxCols(), rst.maxRows(), rst.aggregate(), 
						Set.of(rst.excludeResources()), Set.of(rst.excludeDialects()))
				: store;
	}

	private static <T> T cacheAttribute(NativeWebRequest webRequest, Class<T> clazz, Supplier<? extends T> supplier) {
		var att = webRequest.getAttribute(clazz.getName(), 0);
		if(isNull(att)) {
			att = supplier.get();
			webRequest.setAttribute(clazz.getName(), att, 0);
		}
		return clazz.cast(att);
	}

	private static void resolveParameterCompatibility(Map<String, String[]> modifiableMap) {
		Map.of("column", SELECT_PARAM, "filter", CRITERIA_OPR).entrySet().forEach(e-> {
			var args = modifiableMap.remove(e.getKey());
			if(!isEmpty(args)) {
				log.warn("'{}' parameter is deprecated, use {} instead", e.getKey(), e.getValue());
				modifiableMap.compute(e.getValue(), (k, v)-> isEmpty(v) 
						? args 
						: concat(stream(v), stream(args)).toArray(String[]::new));
			}
		});
	}
}