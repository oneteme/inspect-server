package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.server.model.PartitionBy;
import org.usf.inspect.server.model.PartitionedTable;

import java.time.YearMonth;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScriptService {
    private final JdbcTemplate template;

    @Transactional(rollbackFor = Throwable.class)
    public void createPartitions(YearMonth start, YearMonth end, Map<PartitionedTable, PartitionBy> map) {
        var partitions = PartitionBy.buildPartitionScript(start, end, map);
        template.batchUpdate(partitions.toArray(String[]::new));
    }
}
