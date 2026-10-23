package org.usf.inspect.server;

import static org.usf.inspect.InspectCoreModule.jsonMapperBuilder;

import org.usf.inspect.Retention;
import org.usf.inspect.server.dto.BrowserConfigDto;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class JsonUtils {
	
	public static final JsonMapper defaultMapper;
    
	public static <T> T fromJson(String json, Class<T> valueType) {
        return defaultMapper.readValue(json, valueType);
    }

    public static String toJson(Object object) {
        return defaultMapper.writeValueAsString(object);
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

		var mapper = jsonMapperBuilder() //use same builder as core
				.addModules(serverModule)
				.enable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)
				.enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS) //Write dates as timestamps (default: false)
				.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES); // skip null for primitive types, e.g. int, long, boolean, etc. (default: true)
		defaultMapper = mapper.build();
	}
}
