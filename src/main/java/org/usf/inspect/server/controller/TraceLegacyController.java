package org.usf.inspect.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.SessionContextManager;
import org.usf.inspect.server.model.Session;

import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.SessionContextManager.*;
import static org.usf.inspect.server.controller.RetroUtils.toV4;
import static org.usf.inspect.server.controller.TraceController.update;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "v3/trace", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TraceLegacyController {

    private final TraceController tracer;

    @PostMapping(value = "instance", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addInstanceEnvironment(
            HttpServletRequest hsr,
            @RequestBody InstanceEnvironment instance) {
        instance = update(instance, instance.getType() == SERVER ? instance.getAddress() : hsr.getRemoteAddr(), nextId());
        return tracer.addInstanceEnvironment(hsr, instance);
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Object> addSessions(
            @PathVariable String id,
            @RequestParam(required = false) Integer pending,
            @RequestParam(required = false) Instant end,
            @RequestBody Session[] sessions) { //maybe null !
        return tracer.addSessions(id, pending, -1, null, end, toV4(sessions));
    }
}
