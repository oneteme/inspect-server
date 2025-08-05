package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.model.Session;

import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.usf.inspect.core.InstanceType.CLIENT;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.inspect.server.controller.RetroUtils.toV4;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceController {

    private final TraceControllerV4 traceControllerV4;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
            HttpServletRequest hsr,
            @RequestBody InstanceEnvironment instance) {
        if(instance.getType() != CLIENT) {
            instance = TraceControllerV4.update(instance, instance.getAddress(), nextId());
        }
        return traceControllerV4.addInstanceEnvironment(hsr, instance);
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(
            @PathVariable("id") String id,
            @RequestParam(required = false, name = "pending")  Integer pending,
            @RequestParam(required = false, name = "end") Instant end,
            @RequestBody Session[] sessions) {
	        return traceControllerV4.addSessions(id, pending, -1, end, null, toV4(sessions, id));
    }
}
