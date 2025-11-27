package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.usf.jquery.core.Utils.isBlank;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v4/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private final EventTraceScheduledDispatcher dispatcher;

	private static final Predicate<String> isUUID = compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$").asPredicate();
    
    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
		HttpServletRequest hsr,
		@RequestBody InstanceEnvironment instance) {
    	if(isBlank(instance.getName())) {
    		return status(BAD_REQUEST).body("invalid instance name");
    	}
		var id = assertUUID(instance.getId(), "instance.id");
		try {
			return dispatcher.dispatch(instance)
					? ok(id)
					: status(SERVICE_UNAVAILABLE).body("dispatcher.state=" + dispatcher.getState());
		} catch(Exception e) {
			log.error("post instance", e);
			return internalServerError().body("unexpected exception " + e.getClass().getSimpleName());
		}
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Object> addSessions(
    		@PathVariable String id,
            @RequestParam(required = false) Integer pending,
            @RequestParam(required = false) Integer attempts,
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) Instant end,
            @RequestBody List<EventTrace> traces) {
    	var now = now();
    	var retry = true;
		assertUUID(id, "instance/id");
    	try {
    		if(isNull(traces)) {
    			traces = new ArrayList<>();
    		}
    		int size = traces.size();
    		traces.add(new InstanceTrace(pending, attempts, size, filename, now, id));
    		if(nonNull(end)){
    			traces.add(new InstanceEnvironmentUpdate(id, end));
    		}
    		for(var e : traces) {
    			if(e instanceof AbstractRequest2 req) {
    				req.setInstanceId(id);
					assertUUID(req.getId(), "req.id");
    			} else if(e instanceof AbstractSession2 ses) {
    				ses.setInstanceId(id);
					assertUUID(ses.getId(), "ses.id");
    			} else if(e instanceof MachineResourceUsage usg) {
    				usg.setInstanceId(id);
    			} else if(e instanceof LogEntry ent) {
    				ent.setInstanceId(id);
    			}
    		}
    		retry = false;
    		return dispatcher.emitAll(traces)
    				? accepted().build()
    				: status(SERVICE_UNAVAILABLE).body(new TraceFail(dispatcher.getState().toString(), true));
    	}
    	catch (Throwable e) { //OutOfMem
    		log.error("put sessions", e);
    		return internalServerError().body(new TraceFail(dispatcher.getState().toString(), retry));
    	}
    }
    
    @GetMapping("queue")
    public List<EventTrace> peekQueue(){
		return dispatcher.peek();
    }

    @PostMapping("state/{state}")
    public void updateState(@PathVariable BasicDispatchState state){
		log.info("update dispatcher state to {}", state);
		dispatcher.setState(state);
    }

    static InstanceEnvironment update(InstanceEnvironment instance, String addr, String id) {
    	var ins = new InstanceEnvironment(id, instance.getInstant(), instance.getType(),
                instance.getName(), instance.getVersion(), instance.getEnv(), addr,
                instance.getOs(), instance.getRe(), instance.getUser(), instance.getBranch(),
                instance.getHash(), instance.getCollector(),
                instance.getAdditionalProperties(), instance.getConfiguration());
        ins.setResource(instance.getResource());
        ins.setEnd(instance.getEnd());
    	return ins;
    }

	static String assertUUID(String uuid, String name) {
		if (nonNull(uuid) && isUUID.test(uuid)) {
			return uuid;
		}
		throw new IllegalArgumentException(name + " is not a valid UUID: " + uuid);
	}
}
