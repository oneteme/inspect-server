package org.usf.inspect.server.agent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/** Java JSON-RPC bridge to the headless GitHub Copilot CLI. */
@Slf4j
@Component
public class CopilotCliClient {
	private final AgentProperties properties;
	private final ObjectMapper mapper;
	private final AtomicLong requestIds = new AtomicLong();
	private final Map<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
	private final Map<String, PendingReply> pendingReplies = new ConcurrentHashMap<>();	
	private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();
	private final AtomicReference<Process> process = new AtomicReference<>();
	private final Object writeLock = new Object();
	private BufferedOutputStream output;

	public CopilotCliClient(AgentProperties properties, ObjectMapper mapper) {
		this.properties = properties;
		this.mapper = mapper;
	}

	public synchronized void start() {
		if (process.get() != null && process.get().isAlive()) {
			return;
		}
		try {
			Process startedProcess = new ProcessBuilder(copilotCommand()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
			process.set(startedProcess);
			output = new BufferedOutputStream(startedProcess.getOutputStream());
			Thread reader = new Thread(() -> readMessages(startedProcess), "copilot-rpc-reader");
			reader.setDaemon(true);
			reader.start();
			request("ping", Map.of("message", "inspect-server"), Duration.ofSeconds(15));
			log.info("GitHub Copilot CLI connected for agent API");
		} catch (Exception e) {
			stopProcess();
			throw new IllegalStateException("Unable to start GitHub Copilot CLI. Install and authenticate the 'copilot' CLI, or set agent.copilot-cli-path.", e);
		}
	}

	private List<String> copilotCommand() {
		List<String> arguments = new ArrayList<>();
		String cliPath = properties.getCopilotCliPath();
		boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
		if (windows && !cliPath.toLowerCase(Locale.ROOT).endsWith(".exe")) {
			arguments.add("cmd.exe");
			arguments.add("/d");
			arguments.add("/s");
			arguments.add("/c");
			String command = quoteForCmd(cliPath) + " --headless --no-auto-update --log-level info --stdio";
			arguments.add("\"" + command + "\"");
		} else {
			arguments.add(cliPath);
			arguments.add("--headless");
			arguments.add("--no-auto-update");
			arguments.add("--log-level");
			arguments.add("info");
			arguments.add("--stdio");
		}
		return arguments;
	}

	private String quoteForCmd(String value) {
		return value.contains(" ") ? "\"" + value + "\"" : value;
	}

	public void createSession(String sessionId, String model, Map<String, Object> mcpServers) {
		ObjectNode params = mapper.createObjectNode().put("sessionId", sessionId).put("requestPermission", true)
				.put("envValueMode", "direct");
		if (model != null && !model.isBlank()) {
			params.put("model", model);
		}
		params.set("mcpServers", mapper.valueToTree(mcpServers));
		request("session.create", params, properties.getRequestTimeout());
		activeSessions.add(sessionId);
	}

	public String sendAndWait(String sessionId, String prompt) {
		PendingReply reply = new PendingReply();
		if (pendingReplies.putIfAbsent(sessionId, reply) != null) {
			throw new IllegalStateException("A message is already being processed for this session");
		}
		try {
			request("session.send", Map.of("sessionId", sessionId, "prompt", prompt), properties.getRequestTimeout());
			return reply.result.get(properties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Timed out waiting for Copilot to finish the response", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for Copilot", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Copilot failed to process the message", e.getCause());
		} finally {
			pendingReplies.remove(sessionId, reply);
		}
	}

	public JsonNode getMessages(String sessionId) {
		return request("session.getMessages", Map.of("sessionId", sessionId), properties.getRequestTimeout());
	}

	public void destroySession(String sessionId) {
		request("session.destroy", Map.of("sessionId", sessionId), properties.getRequestTimeout());
		activeSessions.remove(sessionId);
	}

	public void deleteSession(String sessionId) {
		request("session.delete", Map.of("sessionId", sessionId), properties.getRequestTimeout());
		activeSessions.remove(sessionId);
	}

	private JsonNode request(String method, Object params, Duration timeout) {
		startIfNeeded();
		try {
			return requestAsync(method, params, timeout).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("Timed out calling Copilot RPC " + method, e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted calling Copilot RPC " + method, e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("Unable to call Copilot RPC " + method, e.getCause());
		}
	}

	// Never wait on the reader thread: it must remain free to receive the RPC response.
	private CompletableFuture<JsonNode> requestAsync(String method, Object params, Duration timeout) {
		long id = requestIds.incrementAndGet();
		CompletableFuture<JsonNode> response = new CompletableFuture<>();
		pendingRequests.put(id, response);
		response.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
				.whenComplete((result, error) -> pendingRequests.remove(id, response));
		ObjectNode message = mapper.createObjectNode().put("jsonrpc", "2.0").put("id", id).put("method", method);
		message.set("params", mapper.valueToTree(params));
		try {
			writeMessage(message);
		} catch (IOException e) {
			response.completeExceptionally(e);
		}
		return response.thenApply(result -> {
			if (result.has("error")) {
				throw new IllegalStateException("Copilot RPC " + method + " failed");
			}
			return result.path("result");
		});
	}

	private void startIfNeeded() {
		Process currentProcess = process.get();
		if (currentProcess == null || !currentProcess.isAlive()) {
			start();
		}
	}

	private void writeMessage(JsonNode message) throws IOException {
		byte[] body = mapper.writeValueAsBytes(message);
		byte[] header = ("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
		synchronized (writeLock) {
			if (process.get() == null || !process.get().isAlive()) {
				throw new IOException("Copilot CLI process is not running");
			}
			output.write(header);
			output.write(body);
			output.flush();
		}
	}

	private void readMessages(Process cliProcess) {
		try (InputStream stream = new BufferedInputStream(cliProcess.getInputStream())) {
			while (cliProcess.isAlive()) {
				int contentLength = readContentLength(stream);
				if (contentLength < 0) {
					break;
				}
				byte[] body = stream.readNBytes(contentLength);
				if (body.length != contentLength) {
					throw new IOException("Truncated Copilot CLI message");
				}
				JsonNode message = mapper.readTree(body);
				handleMessage(message);
			}
		} catch (Exception e) {
			if (cliProcess.isAlive()) {
				log.error("Error reading Copilot CLI protocol stream", e);
			}
		} finally {
			pendingRequests.values().forEach(future -> future.completeExceptionally(new IllegalStateException("Copilot CLI connection closed")));
			pendingReplies.values().forEach(reply -> reply.result.completeExceptionally(new IllegalStateException("Copilot CLI connection closed")));
		}
	}

	private int readContentLength(InputStream stream) throws IOException {
		int contentLength = -1;
		String line;
		while ((line = readHeaderLine(stream)) != null && !line.isEmpty()) {
			if (line.regionMatches(true, 0, "Content-Length:", 0, 15)) {
				contentLength = Integer.parseInt(line.substring(15).trim());
			}
		}
		if (line == null && contentLength < 0) {
			return -1;
		}
		if (contentLength < 0 || contentLength > 16 * 1024 * 1024) {
			throw new IOException("Missing or invalid Content-Length in Copilot CLI message");
		}
		return contentLength;
	}

	private String readHeaderLine(InputStream stream) throws IOException {
		StringBuilder line = new StringBuilder();
		int previous = -1;
		while (line.length() <= 8192) {
			int value = stream.read();
			if (value < 0) {
				return line.isEmpty() ? null : line.toString();
			}
			if (previous == '\r' && value == '\n') {
				line.setLength(line.length() - 1);
				return line.toString();
			}
			line.append((char) value);
			previous = value;
		}
		throw new IOException("Copilot CLI message header is too large");
	}

	private void handleMessage(JsonNode message) throws IOException {
		if (message.has("method")) {
			String method = message.path("method").asText();
			JsonNode params = message.path("params");
			if ("session.event".equals(method)) {
				handleSessionEvent(params);
			} else if (message.has("id")) {
				handleServerRequest(message.path("id"), method, params);
			}
		} else if (message.has("id")) {
			JsonNode id = message.path("id");
			if (id.canConvertToLong()) {
				CompletableFuture<JsonNode> pending = pendingRequests.get(id.asLong());
				if (pending != null) {
					pending.complete(message);
				}
			}
		}
	}

	private void handleSessionEvent(JsonNode params) {
		String sessionId = params.path("sessionId").asText();
		JsonNode event = params.path("event");
		PendingReply pending = pendingReplies.get(sessionId);
		JsonNode data = event.path("data");
		if ("permission.requested".equals(event.path("type").asText())) {
			ObjectNode permission = mapper.createObjectNode().put("sessionId", sessionId);
			permission.set("requestId", data.path("requestId"));
			permission.set("result", permissionDecision(sessionId, data.path("permissionRequest")));
			requestAsync("session.permissions.handlePendingPermissionRequest", permission, properties.getRequestTimeout())
					.whenComplete((result, error) -> {
						if (error != null && pending != null) {
							pending.result.completeExceptionally(new IllegalStateException("Unable to respond to Copilot permission request"));
						}
					});
			return;
		}
		if (pending == null) {
			return;
		}
		String type = event.path("type").asText();
		if ("assistant.message".equals(type)) {
			pending.answer = data.path("content").asText(null);
		} else if ("session.error".equals(type)) {
			pending.result.completeExceptionally(new IllegalStateException(data.path("message").asText("Copilot session failed")));
		} else if ("session.idle".equals(type)) {
			if (pending.answer == null || pending.answer.isBlank()) {
				pending.result.completeExceptionally(new IllegalStateException("Copilot returned no answer"));
			} else {
				pending.result.complete(pending.answer);
			}
		}
	}

	private void handleServerRequest(JsonNode id, String method, JsonNode params) throws IOException {
		ObjectNode response = mapper.createObjectNode().put("jsonrpc", "2.0");
		response.set("id", id);
		ObjectNode result = mapper.createObjectNode();
		if ("permission.request".equals(method)) {
			result.set("result", permissionDecision(params.path("sessionId").asText(), params.path("permissionRequest")));
		} else if ("tool.call".equals(method)) {
			ObjectNode toolResult = mapper.createObjectNode().put("textResultForLlm", "Tool is not handled by this Java agent")
					.put("resultType", "failure").put("error", "No custom tools are registered");
			toolResult.set("toolTelemetry", mapper.createObjectNode());
			result.set("result", toolResult);
		} else {
			response.set("error", mapper.createObjectNode().put("code", -32601).put("message", "Method not supported: " + method));
		}
		if (!response.has("error")) {
			response.set("result", result);
		}
		writeMessage(response);
	}

	private ObjectNode permissionDecision(String sessionId, JsonNode permission) {
		boolean approved = activeSessions.contains(sessionId) && "mcp".equals(permission.path("kind").asText());
		return mapper.createObjectNode().put("kind", approved
				? "approve-once" : "user-not-available");
	}

	public boolean isRunning() {
		Process currentProcess = process.get();
		return currentProcess != null && currentProcess.isAlive();
	}

	@PreDestroy
	public synchronized void stop() {
		stopProcess();
	}

	private void stopProcess() {
		Process currentProcess = process.getAndSet(null);
		if (currentProcess != null) {
			currentProcess.destroy();
			try {
				if (!currentProcess.waitFor(2, TimeUnit.SECONDS)) {
					currentProcess.destroyForcibly();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				currentProcess.destroyForcibly();
			}
		}
	}

	private static final class PendingReply {
		private final CompletableFuture<String> result = new CompletableFuture<>();
		private volatile String answer;
	}
}
