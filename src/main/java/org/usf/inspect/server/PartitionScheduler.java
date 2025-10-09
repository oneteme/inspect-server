package org.usf.inspect.server;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.model.Partition;
import org.usf.inspect.server.model.PartitionedTable;
import org.usf.inspect.server.service.ScriptService;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.usf.inspect.server.model.PartitionedTable.*;

@Service
@Slf4j
@Setter
@ConditionalOnProperty(prefix = "inspect.server.partition", name="enabled", havingValue = "true")
public class PartitionScheduler {

    private final ScriptService scriptService;
    private final PartitionProperties properties;

    PartitionScheduler(InspectServerConfiguration conf, ScriptService scriptService) {
        this.scriptService = scriptService;
        this.properties = conf.getPartition();
    }

    @Scheduled(cron= "${inspect.server.partition.schedule:0 0 0 L * ?}")
    @TraceableStage
    public void createPartition(){
        YearMonth now = YearMonth.now().plusMonths(1);
        scriptService.createPartitions(now, now, toConfigMap());
    }

    private Map<PartitionedTable,Partition> toConfigMap(){
        Map<PartitionedTable, Partition> map = new EnumMap<>(PartitionedTable.class);
        ofNullable(properties.getHttpSession()).ifPresent(o-> map.put(SES_HTTP, o)); //TD compute => default = Month
        ofNullable(properties.getMainSession()).ifPresent(o-> map.put(SES_MAIN, o));
        ofNullable(properties.getHttpRequest()).ifPresent(o-> map.put(REQ_HTTP, o));
        ofNullable(properties.getJdbcRequest()).ifPresent(o-> map.put(REQ_JDBC, o));
        ofNullable(properties.getFtpRequest()).ifPresent(o-> map.put(REQ_FTP, o));
        ofNullable(properties.getSmtpRequest()).ifPresent(o-> map.put(REQ_SMTP, o));
        ofNullable(properties.getLdapRequest()).ifPresent(o-> map.put(REQ_LDAP, o));
        ofNullable(properties.getLocalRequest()).ifPresent(o-> map.put(REQ_LOCAL, o));
        ofNullable(properties.getInstanceTrace()).ifPresent(o-> map.put(INSTANCE_TRACE, o));
        ofNullable(properties.getResourceUsage()).ifPresent(o-> map.put(RESOURCE_USAGE, o));
        return map;
    }
}
