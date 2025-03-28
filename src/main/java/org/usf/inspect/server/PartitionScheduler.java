package org.usf.inspect.server;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import static org.usf.inspect.server.model.PartitionedTable.REQ_LOCAL;

@Service
@Slf4j
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "inspect.partition")
@ConditionalOnProperty(prefix = "inspect.partition", name="enabled", havingValue = "true")
public class PartitionScheduler {

    private final ScriptService scriptService;

    private SessionPartitionConfiguration session;
    private RequestPartitionConfiguration request;

    @Scheduled(cron= "${inspect.partition.schedule:0 0 0 L * ?}")
    @TraceableStage
    public void createPartition(){
        YearMonth now = YearMonth.now().plusMonths(1);
        scriptService.createPartitions(now,now,toConfigMap());
    }

    private Map<PartitionedTable,Partition> toConfigMap(){
        Map<PartitionedTable, Partition> map = new EnumMap<>(PartitionedTable.class);
        ofNullable(session.getHttp()).ifPresent(o-> map.put(SES_HTTP, o));
        ofNullable(session.getMain()).ifPresent(o-> map.put(SES_MAIN, o));
        ofNullable(request.getHttp()).ifPresent(o-> map.put(REQ_HTTP, o));
        ofNullable(request.getJdbc()).ifPresent(o-> map.put(REQ_JDBC, o));
        ofNullable(request.getFtp()).ifPresent(o-> map.put(REQ_FTP, o));
        ofNullable(request.getSmtp()).ifPresent(o-> map.put(REQ_SMTP, o));
        ofNullable(request.getLdap()).ifPresent(o-> map.put(REQ_LDAP, o));
        ofNullable(request.getLocal()).ifPresent(o-> map.put(REQ_LOCAL, o));
        return map;
    }




}
