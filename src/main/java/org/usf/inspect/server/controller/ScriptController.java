package org.usf.inspect.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.inspect.server.service.ScriptService;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.time.YearMonth;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.*;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "script", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService service;

    @PutMapping(value = "partition")
    public ResponseEntity<?> createPartition(@RequestParam YearMonth start, @RequestParam YearMonth end) {
        if(end.compareTo(start) < 0) {
            return badRequest().body("end < start");
        }
        return ok(service.createPartitions(start, end));
    }
}
