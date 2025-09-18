package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.dao.PurgeDao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurgeService {
    private final PurgeDao purgeDao;

    private interface PurgeStrategy {
        void execute();
    }

    private class BatchPurgeStrategy implements PurgeStrategy {
        @Override
        public void execute() {
            var now = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
            List<InstanceEnvironment> instances = purgeDao.getInstances(null);
            for (InstanceEnvironment instance : instances) {
                if(instance.getConfiguration() != null) {
                    Instant dateLimit = now.minus(instance.getConfiguration().getTracing().getRemote().getRetentionMaxAge());
                    var deleted = purgeDao.purgeByInstance(dateLimit, instance.getEnv(), instance.getName());
                    log.info("[{}]:{} => {} rows deleted before {}", instance.getEnv(), instance.getName(), IntStream.of(deleted).sum(), dateLimit);
                } else {
                    log.info("[{}]:{} => no configuration", instance.getEnv(), instance.getName());
                }
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
                log.info("[{}]:{} => {} rows deleted before {}", env, app, IntStream.of(deleted).sum(), dateLimit);
            }
        }
    }

    private void performPurge(PurgeStrategy strategy) {
        log.info("[start] => purging old data");
        try {
            strategy.execute();
        } catch(Exception e) {
            log.error("[error] => purging old data", e);
        } finally {
            var deleted = purgeDao.finalizePurge();
            log.info("[finalize] => {} rows deleted", IntStream.of(deleted).sum());
            log.info("[vacuum] => purging old data");
            purgeDao.vacuumAnalyze();
        }
        log.info("[end] => purging old data");
    }

    public void purge() {
        performPurge(new BatchPurgeStrategy());
    }

    public void purge(String env, List<String> apps, Instant dateLimit) {
        performPurge(new TargetedPurgeStrategy(env, Objects.isNull(apps) || apps.isEmpty() ? purgeDao.getInstances(env).stream().map(InstanceEnvironment::getName).toList() : apps, dateLimit));
    }
}
