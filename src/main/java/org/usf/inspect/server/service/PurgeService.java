package org.usf.inspect.server.service;

import static java.lang.Thread.ofVirtual;
import static java.sql.Timestamp.from;
import static java.time.LocalDate.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.core.SessionContextManager.emitInfo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.IntSupplier;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.dao.PurgeDao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeService {

    // Use virtual threads to create a lightweight per-task executor
    // Provide named virtual threads so they are easier to identify in logs and thread dumps
    private final ExecutorService technicalExecutor = wrap(virtualThreadExecutor("inspect-purge-technical", 5));
    private final ExecutorService functionalExecutor = wrap(virtualThreadExecutor("inspect-purge-functional", 5));

    private final PurgeDao purgeDao;

    public void launchPurge() {
        log.info("------ Purge start ------");
        try {
            var now = now().atStartOfDay().atZone(systemDefault()).toInstant();
            var instances = purgeDao.selectInstances();
            log.info("------ Purge ------ method=selected, label=Instance, rows={}", instances.size());
            emitInfo("method=select, label=Instance, rows=" + instances.size());
            var tasks = new ArrayList<CompletableFuture<Void>>(instances.size());
            for (InstanceEnvironment instance : instances) {
                Timestamp dateLimit = from(now.minus(instance.getConfiguration().getTracing().getRemote().getRetentionMaxAge()));
                var ids = purgeDao.selectInstanceIds(dateLimit, instance.getEnv(), instance.getName(), instance.getType());
                log.info("------ Purge ------ method=selected, label=InstanceId, rows={}, app={}, env={}, date={}", ids.size(), instance.getName(), instance.getEnv(), dateLimit);
                emitInfo("method=select, label=InstanceId, rows=" + ids.size() + ", app=" + instance.getName() + ", env=" + instance.getEnv() + ", date=" + dateLimit);
                tasks.add(runAsync(runnablePurge(() -> purgeDao.purgeInstance(instance.getEnv(), instance.getName(), dateLimit), "Instance", instance.getEnv(), instance.getName(), dateLimit),  functionalExecutor));
                if(!ids.isEmpty()) {
                    String stringIds = ids.stream().collect(joining("','", "'", "'"));
                    tasks.add(purge(stringIds, dateLimit, instance.getEnv(), instance.getName()));
                }
            }
            allOf(tasks.toArray(new CompletableFuture[0])).join();
        }
        finally {
            log.info("------ Purge finally ------");
            purge().join();
            log.info("------ Purge vacuum ------");
            vacuum().join();
        }
        log.info("------ Purge end ------");
    }

    private CompletableFuture<Void> purge(String ids, Timestamp dateLimit, String env, String app) {
        return allOf(
                runAsync(runnablePurge(() -> purgeDao.purgeInstanceTrace(ids, dateLimit), "InstanceTrace", app, env, dateLimit), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeResourceUsage(ids, dateLimit), "ResourceUsage", app, env, dateLimit), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestSessionStage(ids, dateLimit), "RestSessionStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRestSession(ids, dateLimit), "RestSession", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestRequestStage(ids, dateLimit), "RestRequestStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRestRequest(ids, dateLimit), "RestRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeSmtpRequestStage(ids, dateLimit), "SmtpRequestStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeSmtpRequest(ids, dateLimit), "SmtpRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeFtpRequestStage( ids, dateLimit), "FtpRequestStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeFtpRequest(ids, dateLimit), "FtpRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLdapRequestStage(ids, dateLimit), "LdapRequestStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeLdapRequest(ids, dateLimit), "LdapRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeDtbRequestStage(ids, dateLimit), "JdbcRequestStage", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeDtbRequest(ids, dateLimit), "JdbcRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLocalRequest(ids, dateLimit), "LclRequest", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeMainSession(ids, dateLimit), "MainSession", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLogEntry(ids, dateLimit), "LogEntry", app, env, dateLimit), functionalExecutor)
        );
    }

    private CompletableFuture<Void> purge() {
        return allOf(
                runAsync(runnablePurge(purgeDao::purgeLogEntry, "LogEntry"), functionalExecutor),
                runAsync(runnablePurge(purgeDao::purgeLocalRequest, "LocalRequest"), functionalExecutor),
                runAsync(runnablePurge(purgeDao::purgeMainSession, "MainSession"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeMainSessionStage, "UserAction"), functionalExecutor),
                runAsync(runnablePurge(purgeDao::purgeRestSession, "RestSession"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeRestSessionStage, "RestSessionStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeRestRequest, "RestRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeRestRequestStage, "RestRequestStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeSmtpRequest, "SmtpRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeSmtpRequestStage, "SmtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeFtpRequest, "FtpRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeFtpRequestStage, "FtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeLdapRequest, "LdapRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeLdapRequestStage, "LdapRequestStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeDtbRequest, "JdbcRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(purgeDao::purgeDtbRequestStage, "JdbcRequestStage"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeInstanceTrace, "InstanceTrace"), technicalExecutor),
                runAsync(runnablePurge(purgeDao::purgeResourceUsage, "ResourceUsage"), technicalExecutor)
        );
    }

    private CompletableFuture<Void> vacuum() {
        return allOf(purgeDao.vacuumTables()
        		.map(r-> runAsync(r, technicalExecutor))
        		.toArray(CompletableFuture[]::new));
    }

    private Runnable runnablePurge(IntSupplier action, String label, String app, String env, Timestamp dateLimit) {
        return () -> {
            var sum = action.getAsInt();
            if (dateLimit != null) {
                log.info("------ Purge ------ method=delete, label={}, rows={}, app={}, env={}, date={}", label, sum, app, env, dateLimit);
                emitInfo("method=delete, label=" + label + ", rows=" + sum + ", app=" + app + ", env=" + env + ", date=" + dateLimit);
            }
            else {
                log.info("------ Purge ------ method=delete, label={}, rows={}", label, sum);
                emitInfo("method=delete, label=" + label + ", rows=" + sum);
            }
        };
    }

    private Runnable runnablePurge(IntSupplier action, String label) {
        return runnablePurge(action, label, null, null, null);
    }
    
    public static ExecutorService virtualThreadExecutor(String name, int size) {
        return newFixedThreadPool(size, ofVirtual().name(name + "-", 0).factory());
    }
}


