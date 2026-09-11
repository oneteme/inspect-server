package org.usf.inspect.server;

import static java.util.Objects.nonNull;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.usf.inspect.core.InspectConfiguration.coreModule;

import org.usf.inspect.core.Retention;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TracePacket;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class JsonUtils {
	
	public static final ObjectMapper defaultMapper;
    
	public static <T> T fromJson(String json, Class<T> valueType) {
        if(nonNull(json)) {
            try {
                return defaultMapper.readValue(json, valueType);
            } catch (JsonProcessingException e) {
                log.warn("error while reading value " + valueType, e);
            }
		}
        return null;
    }

    public static String toJson(Object object) {
        if(nonNull(object)) {
            try {
                return defaultMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                log.warn("error while writing value as string", e);
            }
        }
        return null;
    }
    
	static {
		var mapper = json()
				.modules(new JavaTimeModule(), new ParameterNamesModule(), coreModule().registerSubtypes(
						new NamedType(TracePacket.class, "inst-trc"), 
						new NamedType(InstanceEnvironmentUpdate.class, "inst-updt")))
				.build()
			    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // !null & !empty
		mapper.configure(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL, true);
		// Deprecated(since = "v1.1", forRemoval = true)
		mapper.registerSubtypes(
				new NamedType(MainSessionWrapper.class, "main"),
				new NamedType(RestSessionWrapper.class, "rest"));
		//Deprecated(since = "v1.2", forRemoval = true)
		SimpleModule retentionModule = new SimpleModule();
		retentionModule.addDeserializer(Retention.class, new RetentionConfigDeserializer());
		mapper.registerModule(retentionModule);//register RetentionConfigDeserializer

		defaultMapper = mapper;
	}
}
