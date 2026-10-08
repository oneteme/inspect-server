package org.usf.inspect.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.usf.inspect.server.service.PurgeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="inspect.server.purge", name="enabled", havingValue="true")
public class PurgeScheduler {
	
    private final PurgeService purgeService;

    @Scheduled(cron= "${inspect.server.purge.schedule:0 0 1 * * ?}")
    public void purge() {
        purgeService.launchPurge();
    }
}
