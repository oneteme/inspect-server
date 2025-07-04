package org.usf.inspect.server;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.service.PurgeService;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "inspect.purge")
@ConditionalOnProperty(prefix = "inspect.purge", name="enabled", havingValue = "true")
public class PurgeScheduler {

    private final JdbcTemplate template;
    private final PurgeService purgeService;
    private int depth = 90;
    private Map<String, Integer> env;
    private boolean purged = false;

    @Scheduled(cron= "${inspect.purge.schedule:0 0 1 * * ?}")
    @TraceableStage
    public void purgeBatch(){
        try {
            List<String> envList = template.queryForList("SELECT DISTINCT v1.va_env FROM e_env_ins v1 WHERE v1.va_env IS NOT NULL ORDER BY v1.va_env ASC", String.class);
            if(env != null && !env.isEmpty()){
                env.forEach((envName,d) ->
                {
                    envList.remove(envName);
                    if(d > -1){
                        purged |= purgeService.purgeData(List.of(envName), null, Instant.now().minus(d, ChronoUnit.DAYS), null);
                    }
                });
            }
            if(!envList.isEmpty())
                purged |= purgeService.purgeData(envList, null, Instant.now().minus(depth, ChronoUnit.DAYS), null);
            if(purged){
                purgeService.vaccum();
            }
        }catch (Exception e){
            log.error("Error while purging old data: [Purge BATCH]",e);
        }
    }



}
