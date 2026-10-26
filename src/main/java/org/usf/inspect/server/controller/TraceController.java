package org.usf.inspect.server.controller;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.internalServerError;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.core.TracePublisher.ABORT;
import static org.usf.inspect.core.TracePublisher.RETRY;
import static org.usf.inspect.web.WebUtils.TRACE_RETRY_HEADER;
import static org.usf.jquery.core.Utils.isEmpty;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.EventTrace;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.core.TraceDispatchException;
import org.usf.inspect.server.exception.TraceDispatchProcessingException;
import org.usf.inspect.server.service.TraceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(exposedHeaders = TRACE_RETRY_HEADER)
@RequestMapping(value = "/v5/trace", produces = APPLICATION_JSON_VALUE)
public class TraceController {

	private final TraceService service;

	@PutMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
	public ResponseEntity<Object> addInstanceEnvironment(
			@RequestBody InstanceEnvironment instance, 
			@RequestParam int atm, Principal principal){
		if (isNull(instance.getId())){
			return status(BAD_REQUEST).body("invalid id="+instance.getId());
		}
		if(isEmpty(instance.getName())) {
			return status(BAD_REQUEST).body("invalid name="+instance.getName());
		}
		if(nonNull(principal) && !Objects.equals(principal.getName(), instance.getNamespace())) {
			log.warn("namespace mismatch between principal={} and instance={}", principal.getName(), instance.getNamespace());
			instance.setNamespace(principal.getName()); //override namespace with principal name to avoid security issue
		}
		if(isEmpty(instance.getNamespace())) {
			return status(BAD_REQUEST).body("invalid namespace="+instance.getName());
		}
		try {
			service.addInstance(instance, atm);
			return ok(instance.getId().toString());
		}
		catch (TraceDispatchConflictException e) {
			log.warn("trace dispatch conflict for instance id={}", instance.getId(), e);
			return status(CONFLICT).body(e.getMessage());
		}
		catch (TraceDispatchException e) {
			log.warn("trace dispatch exception for instance id={}", instance.getId(), e);
			return status(SERVICE_UNAVAILABLE).body(e.getMessage());
		}
		catch(Exception e) {
			log.error("failed to dispatch instance environment for id={} name={} atm={}", instance.getId(), instance.getName(), atm, e);
			return internalServerError().body("state=%s, message=".formatted(instance.getId()) + e.getMessage());
		}
	}

	@PutMapping("instance/{id}/session")
	public ResponseEntity<Object> addTraces(
			@PathVariable UUID id,
			@RequestParam int seq, //Integer.MAX_VALUE ~ 68years every 1s
			@RequestParam int atm,
			@RequestParam(required = false) Instant end,
			@RequestBody List<EventTrace> traces){
		try {
			service.addTraces(id, seq, atm, end, traces);
			return accepted().build();
		} 
		catch (TraceDispatchConflictException e) {
			log.warn("trace dispatch conflict for instance id={} seq={}", id, seq, e);
			return status(CONFLICT) //409 => ABORT
					.header(TRACE_RETRY_HEADER, ABORT)
					.body(e.getMessage());
		}
		catch (TraceDispatchProcessingException e) {
			log.warn("trace dispatch processing exception for instance id={} seq={}", id, seq, e);
			return internalServerError()
					.header(TRACE_RETRY_HEADER, e.isRetryable() ? RETRY : ABORT)
					.body(e.getMessage());
		}
		catch (TraceDispatchException e) {
			log.warn("trace dispatch exception for instance id={}", id, e);
			return status(SERVICE_UNAVAILABLE)
					.header(TRACE_RETRY_HEADER, RETRY)
					.body(e.getMessage());
		}
		catch (Exception e) {
			log.error("failed to dispatch traces for instance id={} seq={} atm={} end={} traces.size={}", id, seq, atm, end, traces.size(), e);
			return internalServerError()
					.header(TRACE_RETRY_HEADER, RETRY)
					.body(e.getMessage());
		}
	}

	@GetMapping("queue")
	public Collection<EventTrace> peekQueue(){
		return service.peekQueue();
	}

	@PostMapping("state/{state}")
	public void updateState(@PathVariable DispatchState state){
		service.updateState(state);
	}
}
