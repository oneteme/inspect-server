package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.usf.inspect.core.InstanceType.SERVER;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.inspect.server.controller.RetroUtils.toV4;
import static org.usf.inspect.server.controller.TraceController.update;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.model.Session;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        if(instance.getType() == SERVER) { //set session id 
            instance = update(instance, instance.getAddress(), nextId());
        }
        return tracer.addInstanceEnvironment(hsr, instance);
    }

    @PutMapping("instance/{id}/session")
    public ResponseEntity<Void> addSessions(
            @PathVariable String id,
            @RequestParam(required = false) Integer pending,
            @RequestParam(required = false) Instant end,
            @RequestBody Session[] sessions) { //maybe null !
        return tracer.addSessions(id, pending, -1, end, null, toV4(sessions));
    }
}
