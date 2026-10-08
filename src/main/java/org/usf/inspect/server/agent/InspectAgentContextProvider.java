package org.usf.inspect.server.agent;

import static java.util.UUID.fromString;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;
import org.usf.inspect.server.service.RequestService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InspectAgentContextProvider {
	private static final String APPLICATION_REPOSITORY = "dev/donnees/stm/produits/asm/inspect-app";
	private static final String APPLICATION_JIRA = "N1T";

	private final RequestService requestService;
	private final ObjectMapper mapper;


	Map<String, Object> resolveSessionDetail(String id) {
		return resolveSessionContext(requestService.getRestSession(fromString(id)));
	}

	Map<String, Object> resolveSessionDetailView(String id) {
		Session session = requestService.getMainSession(fromString(id));
		if (!(session instanceof MainSessionWrapper main)) {
			throw new IllegalStateException("Inspect returned an unexpected main session representation");
		}

		Map<String, Object> instance = main.getInstanceId() == null
				? Map.of() : requestService.getInstanceSummary(main.getInstanceId());
		Map<String, Object> environment = new LinkedHashMap<>();
		environment.put("application_name", valueOrEmpty(instance.get("appName")));
		environment.put("application_version", valueOrEmpty(instance.get("version")));
		environment.put("application_environment", valueOrEmpty(instance.get("environment")));
		environment.put("user_operating_system", valueOrEmpty(instance.get("os")));
		environment.put("user_browser", valueOrEmpty(instance.get("runtime")));
		environment.put("application_namespace", valueOrEmpty(instance.get("namespace")));
		environment.put("application_user", valueOrEmpty(instance.get("user")));
		environment.put("session_user", valueOrEmpty(main.getUser()));
		environment.put("application_repository", APPLICATION_REPOSITORY);
		environment.put("application_jira", APPLICATION_JIRA);

		Map<String, Object> navigation = new LinkedHashMap<>();
		navigation.put("page_loaded", main.getStart() == null ? "" : main.getStart().toString());
		navigation.put("page_unloaded", main.getEnd() == null ? "" : main.getEnd().toString());
		navigation.put("page_title", valueOrEmpty(main.getName()));
		navigation.put("page_url", valueOrEmpty(main.getLocation()));
		navigation.put("page_environment", environment);
		navigation.put("page_requests", requestService.getPageRequests(main.getId(), main.getStart()));
		return Map.of("page_navigation", navigation);
	}

	// Page-specific enrichment is not yet defined for the following resolvers.
	Map<String, Object> resolveDashboard(String id) {
		return Map.of();
	}

	Map<String, Object> resolveRequestSearch(String id) {
		return Map.of();
	}

	Map<String, Object> resolveRequestDetail(String id) {
		return Map.of();
	}

	Map<String, Object> resolveRequestCompare(String id) {
		return Map.of();
	}

	Map<String, Object> resolveSessionSearch(String id) {
		return Map.of();
	}

	Map<String, Object> resolveSessionTree(String id) {
		return Map.of();
	}

	Map<String, Object> resolveSessionCompare(String id) {
		return Map.of();
	}

	Map<String, Object> resolveInstance(String id) {
		return Map.of();
	}

	Map<String, Object> resolveAnalytic(String id) {
		return Map.of();
	}

	Map<String, Object> resolveArchitecture(String id) {
		return Map.of();
	}

	Map<String, Object> resolveServerSupervision(String id) {
		return Map.of();
	}

	Map<String, Object> resolveClientSupervision(String id) {
		return Map.of();
	}

	Map<String, Object> resolveRequestKpi(String id) {
		return Map.of();
	}

	Map<String, Object> resolveSessionKpi(String id) {
		return Map.of();
	}

	Map<String, Object> resolveUnknown(String id) {
		return Map.of();
	}

	private Map<String, Object> resolveSessionContext(Session session) {
		Map<String, Object> summary = summarizeSession(session);
		if (session.getInstanceId() != null) {
			Map<String, Object> instance = new LinkedHashMap<>(requestService.getInstanceSummary(session.getInstanceId()));
			instance.remove("address");
			Object startedAt = instance.remove("start");
			Object endedAt = instance.remove("end");
			if (startedAt != null) {
				instance.put("startedAt", startedAt instanceof Instant instant ? instant.toString() : startedAt);
			}
			if (endedAt != null) {
				instance.put("endedAt", endedAt instanceof Instant instant ? instant.toString() : endedAt);
			}
			summary.put("instance", instance);
		}
		summary.put("exceptions", requestService.getSessionExceptions(session.getId()).stream()
				.map(this::summarizeException).toList());
		return Map.of("session", summary);
	}

	private Map<String, Object> summarizeSession(Session session) {
		Map<String, Object> summary = new LinkedHashMap<>();
		if (session.getStart() != null) {
			summary.put("startedAt", session.getStart().toString());
		}
		if (session.getEnd() != null) {
			summary.put("endedAt", session.getEnd().toString());
			if (session.getStart() != null && !session.getEnd().isBefore(session.getStart())) {
				summary.put("durationMs", Duration.between(session.getStart(), session.getEnd()).toMillis());
			}
		}
		if (session instanceof RestSessionWrapper rest) {
			putIfPresent(summary, "version", rest.getVersion());
			putIfPresent(summary, "branch", rest.getBranch());
			putIfPresent(summary, "method", rest.getMethod());
			putIfPresent(summary, "host", rest.getHost());
			putIfPresent(summary, "path", rest.getPath());
			summary.put("status", rest.getStatus());
			putIfPresent(summary, "contentType", rest.getContentType());
		} else if (session instanceof MainSessionWrapper main) {
			putIfPresent(summary, "name", main.getName());
			putIfPresent(summary, "type", main.getType());
		} else {
			throw new IllegalStateException("Inspect returned an unexpected session representation");
		}
		return summary;
	}

	private Map<String, Object> summarizeException(ExceptionTrace exception) {
		JsonNode serialized = mapper.valueToTree(exception);
		Map<String, Object> summary = new LinkedHashMap<>();
		serialized.fields().forEachRemaining(entry -> {
			String name = entry.getKey().toLowerCase(Locale.ROOT);
			JsonNode value = entry.getValue();
			if ((name.contains("type") || name.contains("message")) && value.isTextual()) {
				summary.put(entry.getKey(), value.asText());
			} else if (name.contains("stack") && value.isArray()) {
				summary.put(entry.getKey(), value);
			}
		});
		return summary;
	}

	private void putIfPresent(Map<String, Object> target, String key, String value) {
		if (value != null && !value.isBlank()) {
			target.put(key, value);
		}
	}

	private String valueOrEmpty(Object value) {
		return value == null ? "" : value.toString();
	}

}
