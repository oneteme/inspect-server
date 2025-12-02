package org.usf.inspect.server.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.usf.inspect.core.ApplicationPropertiesProvider;

import java.util.Properties;

import static java.lang.String.join;
import static java.util.Objects.isNull;


@RequiredArgsConstructor
public final class ApplicationInspectPropertiesProvider implements ApplicationPropertiesProvider {

	@NonNull
	private final Environment env; //application.yml
	private final Properties properties; //git.properties


	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public String getVersion() { return properties.getProperty("git.build.version"); }

	@Override
	public String getBranch() { return properties.getProperty("git.branch"); }

	@Override
	public String getCommitHash() {
		return properties.getProperty("git.commit.id.abbrev");
	}
	
	private String getProperty(String p) {
		return env.getProperty("spring.application." + p);
	}

	@Override
	public String getEnvironment() {
		var envs = env.getActiveProfiles();
		return isNull(envs) ? null : join(",", envs);
	}
}
