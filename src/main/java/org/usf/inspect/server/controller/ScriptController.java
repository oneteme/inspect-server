package org.usf.inspect.server.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.model.Partition;
import org.usf.inspect.server.model.PartitionedTable;
import org.usf.inspect.server.service.ScriptService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for administrative scripts, notably table partitioning maintenance.
 */
@Slf4j
@RestController
@RequestMapping(value = "script", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService service;

    /**
     * Creates database partitions covering the given period.
     *
     * @param config per-table partition configuration overrides, may be {@code null} (defaults to an empty map)
     * @param start the first month (inclusive) to create partitions for
     * @param end the last month (inclusive) to create partitions for
     * @return {@code 400 BAD_REQUEST} if {@code end} is before {@code start}, otherwise {@code 200 OK}
     */
    @PatchMapping(value = "partition")
    public ResponseEntity<?> createPartition(@RequestBody(required = false) Map<PartitionedTable, Partition> config, @RequestParam YearMonth start, @RequestParam YearMonth end) {
        if(end.compareTo(start) < 0) {
            return badRequest().body("end < start");
        }
        service.createPartitions(start, end, config == null ? new HashMap<>() : config);
        return ok().build();
    }
}
