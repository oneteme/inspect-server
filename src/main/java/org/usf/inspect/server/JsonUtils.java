package org.usf.inspect.server;

import static java.util.Objects.nonNull;

import org.usf.inspect.InspectCoreModule;
import org.usf.inspect.Retention;
import org.usf.inspect.server.dto.BrowserConfigDto;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class JsonUtils {
	
	public static final JsonMapper defaultMapper;
    
	public static <T> T fromJson(String json, Class<T> valueType) {
        if(nonNull(json)) {
            try {
                return defaultMapper.readValue(json, valueType);
            } catch (JacksonException e) {
                log.warn("error while reading value " + valueType, e);
            }
		}
        return null;
    }

    public static String toJson(Object object) {
        if(nonNull(object)) {
            try {
                return defaultMapper.writeValueAsString(object);
            } catch (JacksonException e) {
                log.warn("error while writing value as string", e);
            }
        }
        return null;
    }
    
	static {
		var serverModule = new SimpleModule()
				.addDeserializer(Retention.class, new RetentionConfigDeserializer())
				.registerSubtypes(//FIXME: remove this, until v1.2, we need to keep this for backward compatibility
						new NamedType(MainSessionWrapper.class, "main"),
						new NamedType(RestSessionWrapper.class, "rest"),
						new NamedType(TraceBatch.class, "inst-trc"), //TODO: unnecessary, we don't serialize TraceBatch 
						new NamedType(InstanceEnvironmentUpdate.class, "inst-updt"), //TODO: unnecessary, we don't serialize InstanceEnvironmentUpdate
						new NamedType(BrowserConfigDto.class, "400"));

		var mapper = JsonMapper.builder()
				.addModules(new InspectCoreModule(), serverModule)
				.changeDefaultPropertyInclusion(incl -> incl //replace .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .withValueInclusion(JsonInclude.Include.NON_EMPTY)   // for standard 
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY)) // for Maps/Collections
				.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL);
		// Deprecated(since = "v1.1", forRemoval = true)
		defaultMapper = mapper.build();
	}
}
