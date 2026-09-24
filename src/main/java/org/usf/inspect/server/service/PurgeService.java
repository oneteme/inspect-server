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
import static org.usf.inspect.server.Utils.virtualThreadExecutor;
//import static org.usf.inspect.server.Utils.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.IntSupplier;

import org.springframework.stereotype.Service;
import org.usf.inspect.server.dao.PurgeDao;
import org.usf.inspect.server.dao.PurgeDao.PurgeScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.server.model.TraceType;

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
                var beforeTechnical = from(now.minus(scope.retention().getDiagnostic()));
                var beforeFunctional = from(now.minus(scope.retention().getAudit()));

                var idsBefore = beforeTechnical.after(beforeFunctional) ? beforeTechnical : beforeFunctional;
                var ids = purgeDao.selectInstanceIds(idsBefore, scope.namespace(), scope.app(), scope.type());
                log.info("------ Purge ------ method=selected, label=InstanceId, rows={}, app={}, namespace={}, date={}",
                        ids.size(), scope.app(), scope.namespace(), beforeFunctional);
                emitInfo("method=select, label=InstanceId, rows=" + ids.size()
                        + ", app=" + scope.app()
                        + ", namespace=" + scope.namespace()
                        + ", date=" + beforeFunctional);

                tasks.add(runAsync(
                        runnablePurge(() -> purgeDao.purgeInstance(scope.namespace(), scope.app(), beforeFunctional),
                                "Instance", scope.app(), scope.namespace(), beforeFunctional),
                        functionalExecutor));

                if (!ids.isEmpty()) {
                    tasks.add(purge(ids, beforeTechnical, beforeFunctional, scope.namespace(), scope.app()));
                }

                //purge old instances that have not dh-end
                tasks.add(runAsync(
                        runnablePurge(() -> purgeDao.purgeAbandonedInstances(scope.namespace(), scope.app(), beforeFunctional),
                                "AbandonedInstance", scope.app(), scope.namespace(), beforeFunctional),
                        functionalExecutor));

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

    private CompletableFuture<Void> purge(List<UUID> ids, Timestamp beforeTechnical, Timestamp beforeFunctional, String env, String app) {
        return allOf(
                //session
                runAsync(runnablePurge(() -> purgeDao.purgeMainSession(ids, beforeFunctional), "MainSession", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestSession(ids, beforeFunctional), "RestSession", app, env, beforeFunctional), functionalExecutor),
                //request
                runAsync(runnablePurge(() -> purgeDao.purgeLocalRequest(ids, beforeFunctional), "LclRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeDtbRequest(ids, beforeFunctional), "JdbcRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLdapRequest(ids, beforeFunctional), "LdapRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeFtpRequest(ids, beforeFunctional), "FtpRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeSmtpRequest(ids, beforeFunctional), "SmtpRequest", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRestRequest(ids, beforeFunctional), "RestRequest", app, env, beforeFunctional), functionalExecutor),
                //event
                runAsync(runnablePurge(() -> purgeDao.purgeInstanceTrace(ids, beforeFunctional), "InstanceTrace", app, env, beforeFunctional), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeLogEntry(ids, beforeTechnical), "LogEntry", app, env, beforeTechnical), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeResourceUsage(ids, beforeTechnical), "ResourceUsage", app, env, beforeTechnical), technicalExecutor)
        );
    }

    private CompletableFuture<Void> purge() {
        var now = LocalDate.now();
        return allOf(

                //session
                runAsync( runnablePurge(() -> purgeDao.purgeMainSession(now), "MainSession"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeRestSession(now), "RestSession"), functionalExecutor),
                //request
                runAsync(runnablePurge(()-> purgeDao.purgeLocalRequest(now), "LocalRequest"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeRestRequest(now), "RestRequest"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeSmtpRequest(now), "SmtpRequest"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeFtpRequest(now), "FtpRequest"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeLdapRequest(now), "LdapRequest"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeDtbRequest(now), "JdbcRequest"), functionalExecutor),
                //stage
                runAsync(runnablePurge(()-> purgeDao.purgeMainSessionStage(now), "SessionEvent"), functionalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeRestSessionStage(now), "RestSessionStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeSmtpRequestStage(now), "SmtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeFtpRequestStage(now), "FtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeLdapRequestStage(now), "LdapRequestStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeDtbRequestStage(now), "JdbcRequestStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeRestRequestStage(now), "RestRequestStage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeMailRequestStage(now), "MailRequestStage"), technicalExecutor),
                //event
                runAsync(runnablePurge(()-> purgeDao.purgeException(now), "Exception"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeInstanceTrace(now), "InstanceTrace"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeResourceUsage(now), "ResourceUsage"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeLogEntry(now), "LogEntry"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeSessionEvent(now), "SessionEvent"), technicalExecutor),
                runAsync(runnablePurge(()-> purgeDao.purgeBrowserConfig(now), "BrowserConfig"), technicalExecutor)
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