package org.usf.inspect.server.agent;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
	private String copilotCliPath = "copilot";
	private String model;
	private Duration requestTimeout = Duration.ofMinutes(5);
	private Duration sessionTtl = Duration.ofHours(1);
	private Mcp mcp = new Mcp();

	@Getter
	@Setter
	public static class Mcp {
		private String serversJson;
		private String gatewayToken;
		private String githubToken;
		private String confluenceUrl;
		private String placideUrl;
		private String drawioUrl;
		private String playwrightUrl;
		private String jiraUrl;
		private String githubUrl;
	}
}
