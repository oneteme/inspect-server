package org.usf.inspect.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(InspectPartitionConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect.partition", name="enabled", havingValue = "true")
public class PartitionScheduler {

    // partitionService
    private final InspectPartitionConfiguration inspectPartitionConfiguration;

    @Scheduled(cron= "#{@inspectPartitionConfiguration.config.schedule}")
    @TraceableStage
    public void createPartition(){
            log.info(inspectPartitionConfiguration.getConfig().toString());
    }

}
