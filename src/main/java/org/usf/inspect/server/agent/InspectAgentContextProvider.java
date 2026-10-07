package org.usf.inspect.server.agent;

import static java.util.UUID.fromString;

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
	private final RequestService requestService;
	private final ObjectMapper mapper;

	public Map<String, Object> resolve(InspectAgentController.InspectAgentRequest request) {
		Map<String, Object> context = new LinkedHashMap<>();
		putIfPresent(context, "page", request.page());
		putIfPresent(context, "type", request.type());
		putIfPresent(context, "recordId", request.id());

		if (request.id() != null && !request.id().isBlank()) {
			if (isType(request.type(), "rest")) {
				Session session = requestService.getRestSession(fromString(request.id().trim()));
				context.put("session", summarizeSession(session));
			} else if (isType(request.type(), "main")) {
				Session session = requestService.getMainSession(fromString(request.id().trim()));
				context.put("session", summarizeSession(session));
			} else {
				throw new IllegalArgumentException("type must be Rest or Main when id is provided");
			}
		}
		return context;
	}

	private Map<String, Object> summarizeSession(Session session) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("id", session.getId());
		summary.put("instanceId", session.getInstanceId());
		if (session.getInstanceId() != null) {
			summary.put("instance", requestService.getInstanceSummary(session.getInstanceId()));
		}
		summary.put("start", session.getStart());
		summary.put("end", session.getEnd());
		summary.put("requestsMask", session.getRequestsMask()); // todo remove
		if (session instanceof RestSessionWrapper rest) {
			putIfPresent(summary, "appName", rest.getAppName());
			putIfPresent(summary, "version", rest.getVersion());
			putIfPresent(summary, "branch", rest.getBranch());
			putIfPresent(summary, "method", rest.getMethod());
			putIfPresent(summary, "host", rest.getHost());
			putIfPresent(summary, "path", rest.getPath());
			summary.put("status", rest.getStatus());
			putIfPresent(summary, "contentType", rest.getContentType());
		} else if (session instanceof MainSessionWrapper main) {
			putIfPresent(summary, "appName", main.getAppName());
			putIfPresent(summary, "name", main.getName());
			putIfPresent(summary, "type", main.getType());
		} else {
			throw new IllegalStateException("Inspect returned an unexpected session representation");
		}
		summary.put("exceptions", requestService.getSessionExceptions(session.getId()).stream()
				.map(this::summarizeException).toList());
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

	private boolean isType(String actual, String expected) {
		return actual != null && expected.equalsIgnoreCase(actual.trim());
	}
}
