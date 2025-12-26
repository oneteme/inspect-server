package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.SessionContextManager;
import org.usf.inspect.server.dao.PurgeDao;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.time.Duration.*;
import static java.time.LocalDate.*;
import static java.time.ZoneId.*;
import static java.util.Objects.*;
import static org.usf.inspect.core.SessionContextManager.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeService {
    private final PurgeDao purgeDao;

    private interface PurgeStrategy {
        void execute();
    }

    private class BatchPurgeStrategy implements PurgeStrategy {
        @Override
        public void execute() {
            var now = now().atStartOfDay().atZone(systemDefault()).toInstant();
            var instances = purgeDao.getInstances(null);
            for (InstanceEnvironment instance : instances) {
                Instant dateLimit = now.minus(instance.getConfiguration().getTracing().getRemote().getRetentionMaxAge());
                var deleted = purgeDao.purgeByInstance(dateLimit, instance.getEnv(), instance.getName());
                log.info("------ Purge complete ------ [{}]:[{}] — [{}] rows deleted before [{}]", instance.getEnv(), instance.getName(), IntStream.of(deleted).sum(), dateLimit);
                emitInfo("Purge completed for instance [" + instance.getEnv() + "]:[" + instance.getName() + "] — [" + IntStream.of(deleted).sum() + "] rows deleted before [" + dateLimit + "]");
            }
        }
    }

    private class TargetedPurgeStrategy implements PurgeStrategy {
        private final String env;
        private final List<String> apps;
        private final Instant dateLimit;

        public TargetedPurgeStrategy(String env, List<String> apps, Instant dateLimit) {
            this.env = env;
            this.apps = apps;
            this.dateLimit = dateLimit;
        }

        @Override
        public void execute() {
            for (String app : apps) {
                var deleted = purgeDao.purgeByInstance(dateLimit, env, app);
                log.info("------ Purge complete ------ [{}]:[{}] — [{}] rows deleted before [{}]", env, app, IntStream.of(deleted).sum(), dateLimit);
                emitInfo("Purge completed for instance [" + env + "]:[" + app + "] — [" + IntStream.of(deleted).sum() + "] rows deleted before [" + dateLimit + "]");
            }
        }
    }

    private void performPurge(PurgeStrategy strategy) {
        log.info("------ Purge start ------");
        try {
            log.info("------ Purge execute ------");
            strategy.execute();
        } finally {
            var deleted = purgeDao.finalizePurge();
            log.info("------ Purge finally ------ [{}] rows deleted", IntStream.of(deleted).sum());
            emitInfo("Purge finally — [" + IntStream.of(deleted).sum() + "] rows deleted");
            log.info("------ Purge vacuum ------");
            purgeDao.vacuumAnalyze();
        }
        log.info("------ Purge end ------");
    }

    public void purge() {
        performPurge(new BatchPurgeStrategy());
    }

    public void purge(String env, List<String> apps, Instant dateLimit) {
        performPurge(new TargetedPurgeStrategy(env, isNull(apps) || apps.isEmpty() ? purgeDao.getInstances(env).stream().map(InstanceEnvironment::getName).toList() : apps, dateLimit));
    }
}
