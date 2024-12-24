package org.usf.inspect.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.service.PurgeService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;



@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inspect.purge", name="enabled", havingValue = "true")
public class PurgeScheduler {

    private final JdbcTemplate template;
    private final PurgeService purgeService;
    private final InspectConfiguration inspectConfiguration;

    @Scheduled(cron= "#{@inspectConfiguration.config.schedule}")
    @TraceableStage
    public void purgeBatch(){
        List<String> envList = template.queryForList("SELECT DISTINCT v1.va_env FROM e_env_ins v1 WHERE v1.va_env IS NOT NULL ORDER BY v1.va_env ASC",String.class);
        var config =  inspectConfiguration.getConfig();
        if(config.getEnv() != null && !config.getEnv().isEmpty()){
            envList = envList.stream().filter(item -> !config.getEnv().containsKey(item)).toList();
            config.getEnv().forEach((envName,depth) -> purgeData(List.of(envName),null,depth,null));
        }
        purgeData(envList,null,config.getDepth(),null);
    }

    public void purgeData(List<String> env, List<String> appName, int depth, List<String> versions){
        Instant threshold = Instant.now().minus(depth, ChronoUnit.DAYS);
        purgeService.purgeData(env, appName, threshold, versions);
    }


}
