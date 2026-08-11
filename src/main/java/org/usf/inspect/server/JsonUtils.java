package org.usf.inspect.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides safe JSON serialization and deserialization helpers.
 */
@Slf4j
public class JsonUtils {

    /**
     * Deserializes the JSON string into the requested type and returns {@code null} on parsing errors.
     *
     * @param json the JSON string to deserialize
     * @param mapper the object mapper used for deserialization
     * @param valueType the target type to create
     * @param <T> the target object type
     * @return the deserialized object, or {@code null} when the input is {@code null} or cannot be parsed
     */
    public static <T> T safeReadValue(String json, ObjectMapper mapper, Class<T> valueType) {
        T result = null;
        try {
            result = json != null ? mapper.readValue(json, valueType) : null;
        } catch (JsonProcessingException e) {
            log.warn("error while reading value " + valueType, e);
        }
        return result;
    }

    /**
     * Serializes the provided object to JSON and returns {@code null} on serialization errors.
     *
     * @param object the object to serialize
     * @param mapper the object mapper used for serialization
     * @return the JSON representation of the object, or {@code null} when the input is {@code null} or cannot be serialized
     */
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
