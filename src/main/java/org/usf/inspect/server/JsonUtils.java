package org.usf.inspect.server;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class JsonUtils {
    
	public static <T> T safeReadValue(String json, Class<T> valueType, ObjectMapper mapper) {
        if(nonNull(json)) {
            try {
                return mapper.readValue(json, valueType);
            } catch (JsonProcessingException e) {
                log.warn("error while reading value " + valueType, e);
            }
		}
        return null;
    }

    public static String safeWriteValue(Object object, ObjectMapper mapper) {
        if(nonNull(object)) {
            try {
                return mapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                log.warn("error while writing value as string", e);
            }
        }
        return null;
    }
}
