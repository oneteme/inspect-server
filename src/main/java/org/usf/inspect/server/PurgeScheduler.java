package org.usf.inspect.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.service.PurgeService;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="inspect.server.purge", name="enabled", havingValue="true")
public class PurgeScheduler {
	
    private final PurgeService purgeService;

    @TraceableStage
    @Scheduled(cron= "${inspect.server.purge.schedule:0 0 1 * * ?}")
    public void purge() {
        purgeService.launchPurge();
    }
}
