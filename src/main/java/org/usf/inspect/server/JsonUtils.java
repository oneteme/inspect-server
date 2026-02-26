package org.usf.inspect.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {
    public static <T> T safeReadValue(String json, ObjectMapper mapper, Class<T> valueType) {
        T result = null;
        try {
            result = json != null ? mapper.readValue(json, valueType) : null;
        } catch (JsonProcessingException e) {
            log.warn("error while reading value " + valueType, e);
        }
        return result;
    }

    public static String safeWriteValue(Object object, ObjectMapper mapper) {
        String result = null;
        try {
            result = object != null ? mapper.writeValueAsString(object) : null;
        } catch (JsonProcessingException e) {
            log.warn("error while writing value as string", e);
        }
        return result;
    }
}
