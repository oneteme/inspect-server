package org.usf.inspect.server.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.service.CompareService;
import org.usf.inspect.server.validation.Condition;
import org.usf.inspect.server.validation.Validate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@Validated
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @GetMapping(value = "request/rest/{id}/compare", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getComparedSessionFromRequest(@PathVariable @Validate(Condition.UUID) String id) {
        try {
            return ok().body(compareService.getComparedSession(id));
        } catch (NoSuchElementException e) {
            return status(NOT_FOUND).body(null);
        }
    }

    @GetMapping(value = "session/rest/{id}/compare", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getComparedSessioFromSession(@PathVariable @Validate(Condition.UUID) String id) {
        try {
            return ok().body(compareService.getComparedSession(id));
        } catch (NoSuchElementException e) {
            return status(NOT_FOUND).body(null);
        }
    }
}

