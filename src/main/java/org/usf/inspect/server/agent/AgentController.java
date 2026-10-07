package org.usf.inspect.server.agent;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {
	private final AgentSessionService sessions;

	@PostMapping("/chat")
	public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
		if (request.message() == null || request.message().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
		}
		try {
			return ResponseEntity.ok(sessions.chat(request.message(), request.sessionId()));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body(Map.of("error", e.getMessage() == null ? "Model request failed" : e.getMessage()));
		}
	}

	@GetMapping("/health")
	public Map<String, Object> health() {
		return sessions.health();
	}

	@GetMapping("/sessions")
	public Map<String, Object> listSessions() {
		return sessions.listSessions();
	}

	@GetMapping("/sessions/{sessionId}")
	public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
		return sessions.history(sessionId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/sessions/{sessionId}")
	public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
		return sessions.deleteSession(sessionId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	public record ChatRequest(String message, String sessionId) { }
}



