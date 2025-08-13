package org.usf.inspect.server.service;

import java.time.YearMonth;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.server.model.Partition;
import org.usf.inspect.server.model.PartitionedTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptService {
    private final JdbcTemplate template;

    @Transactional(rollbackFor = Throwable.class)
    public void createPartitions(YearMonth start, YearMonth end, Map<PartitionedTable, Partition> map) {
        log.info("+ Creating new partitions, parameters in entry"); // change to inline
        log.info("\t- Period: [{}, {}]", start, end);
        log.info("\t- Tables: " + map.toString());
        var partitions = Partition.buildPartitionScript(start, end, map);
        template.batchUpdate(partitions.toArray(String[]::new));
    }
}
