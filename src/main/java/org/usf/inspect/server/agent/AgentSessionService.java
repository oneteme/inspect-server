package org.usf.inspect.server.agent;

import java.time.Instant;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AgentSessionService {
	private final AgentProperties properties;
	private final CopilotCliClient copilot;
	private final ObjectMapper mapper;
	private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
	private final Instant startedAt = Instant.now();

	public AgentSessionService(AgentProperties properties, CopilotCliClient copilot, ObjectMapper mapper) {
		this.properties = properties;
		this.copilot = copilot;
		this.mapper = mapper;
	}

	public Map<String, Object> chat(String message, String requestedSessionId) {
		String sessionId = requestedSessionId != null && sessions.containsKey(requestedSessionId)
				? requestedSessionId : UUID.randomUUID().toString();
		SessionRecord session = sessions.computeIfAbsent(sessionId, this::createSession);
		session.lock.lock();
		try {
			String response = copilot.sendAndWait(session.id, message);
			session.messageCount.addAndGet(2);
			session.lastActivity = Instant.now();
			return Map.of("sessionId", session.id, "response", response, "messageCount", session.messageCount.get());
		} finally {
			session.lock.unlock();
		}
	}

	public boolean hasSession(String sessionId) {
		return sessionId != null && sessions.containsKey(sessionId);
	}

	public Optional<Map<String, Object>> history(String sessionId) {
		SessionRecord session = sessions.get(sessionId);
		if (session == null) {
			return Optional.empty();
		}
		session.lock.lock();
		try {
			JsonNode messages = copilot.getMessages(session.id).path("events");
			session.lastActivity = Instant.now();
			return Optional.of(Map.of("sessionId", session.id, "messages", messages));
		} finally {
			session.lock.unlock();
		}
	}

	public Map<String, Object> listSessions() {
		List<Map<String, Object>> values = new ArrayList<>();
		sessions.values().forEach(session -> values.add(Map.of(
				"id", session.id,
				"messageCount", session.messageCount.get(),
				"createdAt", session.createdAt,
				"lastActivity", session.lastActivity)));
		return Map.of("count", values.size(), "sessions", values);
	}

	public boolean deleteSession(String sessionId) {
		SessionRecord session = sessions.get(sessionId);
		if (session == null) {
			return false;
		}
		session.lock.lock();
		try {
			if (!sessions.remove(sessionId, session)) {
				return false;
			}
			try {
				copilot.destroySession(session.id);
			} finally {
				copilot.deleteSession(session.id);
			}
			return true;
		} finally {
			session.lock.unlock();
		}
	}

	public Map<String, Object> health() {
		Map<String, Object> health = new LinkedHashMap<>();
		health.put("status", "ok");
		health.put("uptime", (Instant.now().toEpochMilli() - startedAt.toEpochMilli()) / 1000.0);
		health.put("copilotConnected", copilot.isRunning());
		return health;
	}

	@Scheduled(fixedDelay = 60_000)
	public void removeExpiredSessions() {
		Instant cutoff = Instant.now().minus(properties.getSessionTtl());
		sessions.values().stream()
				.filter(session -> session.lastActivity.isBefore(cutoff))
				.forEach(session -> {
					try {
						session.lock.lock();
						boolean expired;
						try {
							expired = session.lastActivity.isBefore(Instant.now().minus(properties.getSessionTtl()));
							if (expired) {
								deleteSession(session.id);
							}
						} finally {
							session.lock.unlock();
						}
						if (expired) {
							log.debug("Expired inactive agent session {}", session.id);
						}
					} catch (RuntimeException e) {
						log.warn("Unable to remove expired agent session {}: {}", session.id, e.getMessage());
					}
				});
	}

	private SessionRecord createSession(String id) {
		copilot.createSession(id, properties.getModel(), mcpServers());
		return new SessionRecord(id);
	}

	private Map<String, Object> mcpServers() {
		Map<String, Object> servers = new LinkedHashMap<>();
		AgentProperties.Mcp mcp = properties.getMcp();
		addHttpServer(servers, "confluence", mcp.getConfluenceUrl(), mcp.getGatewayToken());
		addHttpServer(servers, "placide", mcp.getPlacideUrl(), mcp.getGatewayToken());
		addHttpServer(servers, "drawio", mcp.getDrawioUrl(), mcp.getGatewayToken());
		addHttpServer(servers, "playwright", mcp.getPlaywrightUrl(), mcp.getGatewayToken());
		addHttpServer(servers, "jira", mcp.getJiraUrl(), mcp.getGatewayToken());
		addHttpServer(servers, "github", mcp.getGithubUrl(), mcp.getGithubToken());
		addConfiguredServers(servers, mcp.getServersJson());
		return servers;
	}

	private void addConfiguredServers(Map<String, Object> servers, String serversJson) {
		if (serversJson == null || serversJson.isBlank()) {
			return;
		}
		try {
			Map<String, Object> configured = mapper.readValue(serversJson, new TypeReference<>() { });
			for (var entry : configured.entrySet()) {
				if (!(entry.getValue() instanceof Map<?, ?> values)) {
					throw new IllegalArgumentException("MCP server '" + entry.getKey() + "' must be a JSON object");
				}
				Map<String, Object> server = new LinkedHashMap<>();
				values.forEach((key, value) -> server.put(String.valueOf(key), value));
				validateServer(entry.getKey(), server);
				servers.put(entry.getKey(), server);
			}
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException illegalArgumentException) {
				throw illegalArgumentException;
			}
			throw new IllegalArgumentException("MCP_SERVERS must contain a JSON object of named server configurations", e);
		}
	}

	private void validateServer(String name, Map<String, Object> server) {
		String type = String.valueOf(server.getOrDefault("type", server.containsKey("url") ? "http" : "stdio"));
		if (!List.of("http", "sse", "stdio", "local").contains(type)) {
			throw new IllegalArgumentException("Unsupported MCP server type '" + type + "' for '" + name + "'");
		}
		if ("http".equals(type) || "sse".equals(type)) {
			Object url = server.get("url");
			if (!(url instanceof String value) || value.isBlank()) {
				throw new IllegalArgumentException("MCP server '" + name + "' requires a non-empty url");
			}
			String scheme = URI.create(value).getScheme();
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				throw new IllegalArgumentException("MCP server '" + name + "' url must use HTTP or HTTPS");
			}
		} else {
			if (!(server.get("command") instanceof String command) || command.isBlank()) {
				throw new IllegalArgumentException("MCP server '" + name + "' requires a non-empty command");
			}
			if (!(server.get("args") instanceof List<?> args) || args.stream().anyMatch(argument -> !(argument instanceof String))) {
				throw new IllegalArgumentException("MCP server '" + name + "' requires an args array of strings");
			}
		}
		server.putIfAbsent("type", type);
		server.putIfAbsent("tools", List.of("*"));
	}

	private void addHttpServer(Map<String, Object> servers, String name, String url, String token) {
		if (url == null || url.isBlank() || token == null || token.isBlank()) {
			return;
		}
		Map<String, Object> server = new LinkedHashMap<>();
		server.put("type", "http");
		server.put("url", url);
		server.put("headers", Map.of("Authorization", "Bearer " + token.trim()));
		server.put("tools", List.of("*"));
		servers.putIfAbsent(name, server);
	}

	private static final class SessionRecord {
		private final String id;
		private final Instant createdAt = Instant.now();
		private final ReentrantLock lock = new ReentrantLock();
		private volatile Instant lastActivity = createdAt;
		private final AtomicInteger messageCount = new AtomicInteger();

		private SessionRecord(String id) {
			this.id = id;
		}
	}
}
