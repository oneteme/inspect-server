package org.usf.inspect.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.service.PurgeService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(InspectConfigurationProperties.class)
@ConditionalOnProperty(prefix = "inspect.purge", name="enabled", havingValue = "true")
public class PurgeScheduler {

    private final JdbcTemplate template;
    private final PurgeService purgeService;
    private final InspectConfiguration inspectConfiguration;

    @Scheduled(cron= "#{@inspectConfiguration.config.schedule}")
    @TraceableStage
    public void purgeBatch(){
        try {
            List<String> envList = template.queryForList("SELECT DISTINCT v1.va_env FROM e_env_ins v1 WHERE v1.va_env IS NOT NULL ORDER BY v1.va_env ASC", String.class);
            if(inspectConfiguration.getConfig().getEnv() != null && !inspectConfiguration.getConfig().getEnv().isEmpty()){
                inspectConfiguration.getConfig().getEnv().forEach((envName,depth) ->
                {
                    envList.remove(envName);
                    purgeService.purgeData(List.of(envName), null, Instant.now().minus(depth, ChronoUnit.DAYS), null);
                });
            }
            if(!envList.isEmpty())
                purgeService.purgeData(envList, null, Instant.now().minus(inspectConfiguration.getConfig().getDepth(), ChronoUnit.DAYS), null);
        }catch (Exception e){
            log.error("Error while purging old data: [Purge BATCH]",e);
        }

    }



}
