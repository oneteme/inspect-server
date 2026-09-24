package org.usf.inspect.server.config;

import java.util.Properties;

import org.springframework.core.env.Environment;
import org.usf.inspect.core.DefaultApplicationPropertiesProvider;

import lombok.NonNull;

public final class ApplicationInspectPropertiesProvider extends DefaultApplicationPropertiesProvider {

	private final Properties properties; // git.properties

	public ApplicationInspectPropertiesProvider(@NonNull Environment env, Properties properties) {
		super(env);
		this.properties = properties;
	}

	@Override
	public String getVersion() {
		return properties.getProperty("git.build.version");
	}

	@Override
	public String getBranch() {
		return properties.getProperty("git.branch");
	}

	@Override
	public String getCommitHash() {
		return properties.getProperty("git.commit.id.abbrev");
	}
}
