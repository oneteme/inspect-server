package org.usf.inspect.server.service;

import static java.sql.Timestamp.from;
import static java.time.LocalDate.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.core.SessionContextManager.emitInfo;
import static org.usf.inspect.server.Utils.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.IntSupplier;

import org.springframework.stereotype.Service;
import org.usf.inspect.server.dao.PurgeDao;
import org.usf.inspect.server.dao.PurgeDao.PurgeScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeService {

    private final ExecutorService technicalExecutor = wrap(virtualThreadExecutor("inspect-purge-technical", 5));
    private final ExecutorService functionalExecutor = wrap(virtualThreadExecutor("inspect-purge-functional", 5));

    private final PurgeDao purgeDao;

    public void launchPurge() {
        log.info("------ Purge start ------");
        RuntimeException error = null;
        try {
            var now = now().atStartOfDay().atZone(systemDefault()).toInstant();
            var scopes = purgeDao.selectInstances();
            log.info("------ Purge ------ method=selected, label=Instance, rows={}", scopes.size());
            emitInfo("method=select, label=Instance, rows=" + scopes.size());

            var tasks = new ArrayList<CompletableFuture<Void>>(scopes.size());
            for (PurgeScope scope : scopes) {
                var beforeTechnical = from(now.minus(scope.diagnosticRetention()));
                var beforeFunctional = from(now.minus(scope.auditRetention()));

                var idsBefore = beforeTechnical.after(beforeFunctional) ? beforeTechnical : beforeFunctional;
                var ids = purgeDao.selectInstanceIds(idsBefore, scope.env(), scope.app(), scope.type());
                log.info("------ Purge ------ method=selected, label=InstanceId, rows={}, app={}, env={}, date={}",
                        ids.size(), scope.app(), scope.env(), beforeFunctional);
                emitInfo("method=select, label=InstanceId, rows=" + ids.size()
                        + ", app=" + scope.app()
                        + ", env=" + scope.env()
                        + ", date=" + beforeFunctional);

                tasks.add(runAsync(
                        runnablePurge(() -> purgeDao.purgeInstance(scope.env(), scope.app(), beforeFunctional),
                                "Instance", scope.app(), scope.env(), beforeFunctional),
                        functionalExecutor));

                if (!ids.isEmpty()) {
                    var stringIds = ids.stream().collect(joining("','", "'", "'"));
                    tasks.add(purge(stringIds, beforeTechnical, beforeFunctional, scope.env(), scope.app()));
                }
            }

            allOf(tasks.toArray(new CompletableFuture[0])).join();
        } catch (RuntimeException t) {
            log.error("Error during purge", t);
            error = t;
        } finally {
            try {
                log.info("------ Purge finally ------");
                purge().join();
            } catch (RuntimeException e) {
                log.error("Error during purge", e);
                if (isNull(error)) {
                    error = e;
                }
            } finally {
                log.info("------ Purge vacuum ------");
                vacuum().join();
            }
            log.info("------ Purge end ------");
        }
        if (nonNull(error)) {
            throw error;
        }
    }

    private CompletableFuture<Void> purge(String ids, Timestamp beforeTechnical, Timestamp beforeFunctional, String env, String app) {
        return allOf(
                runAsync(runnablePurge(() -> purgeDao.purgeInstanceTrace(ids, beforeTechnical), "InstanceTrace", app, env, beforeTechnical), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeResourceUsage(ids, beforeTechnical), "ResourceUsage", app, env, beforeTechnical), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestSessionStage(ids, beforeTechnical), "RestSessionStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRestSession(ids, beforeFunctional), "RestSession", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestRequestStage(ids, beforeTechnical), "RestRequestStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRestRequest(ids, beforeFunctional), "RestRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeSmtpRequestStage(ids, beforeTechnical), "SmtpRequestStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeSmtpRequest(ids, beforeFunctional), "SmtpRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeFtpRequestStage(ids, beforeTechnical), "FtpRequestStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeFtpRequest(ids, beforeFunctional), "FtpRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLdapRequestStage(ids, beforeTechnical), "LdapRequestStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeLdapRequest(ids, beforeFunctional), "LdapRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeDtbRequestStage(ids, beforeTechnical), "JdbcRequestStage", app, env, beforeTechnical), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeDtbRequest(ids, beforeFunctional), "JdbcRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLocalRequest(ids, beforeFunctional), "LclRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeMainSession(ids, beforeFunctional), "MainSession", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLogEntry(ids, beforeTechnical), "LogEntry", app, env, beforeTechnical), technicalExecutor)
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
                .map(r -> runAsync(r, technicalExecutor))
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
}