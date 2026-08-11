package org.usf.inspect.server.service;

import static org.usf.inspect.core.DispatchState.COLLECT;
import static org.usf.inspect.server.model.Partition.buildPartitionScript;

import java.time.YearMonth;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.DispatchState;
import org.usf.inspect.server.model.Partition;
import org.usf.inspect.server.model.PartitionedTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates database partitions for configured trace tables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final JdbcTemplate template;
    private final TraceService traceService;

    /**
     * Creates partitions for the supplied tables within the specified period.
     *
     * @param start the first month to partition
     * @param end the last month to partition
     * @param map the partition configuration for each table
     */
    @Transactional(rollbackFor = Throwable.class)
    public void createPartitions(YearMonth start, YearMonth end, Map<PartitionedTable, Partition> map) {
        log.info("+ Creating new partitions, parameters in entry"); // change to inline
        log.info("\t- Period: [{}, {}]", start, end);
        log.info("\t- Tables: " + map.toString());

        DispatchState previousState = traceService.getState();
        traceService.updateState(COLLECT);
        try {
            var partitions = buildPartitionScript(start, end, map);
            template.batchUpdate(partitions.toArray(String[]::new));
            log.info("Partitions created successfully");
        } finally {
            traceService.updateState(previousState);
        }
    }
}
