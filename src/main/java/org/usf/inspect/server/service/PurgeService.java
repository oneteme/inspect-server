package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.dao.PurgeDao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.sql.Timestamp.*;
import static java.time.LocalDate.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.*;
import static org.usf.inspect.core.ExecutorServiceWrapper.*;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.core.SessionContextManager.emitInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeService {

    private final ExecutorService technicalExecutor = wrap(newFixedThreadPool(5)); //TODO use virtual thread 
    private final ExecutorService functionalExecutor = wrap(newFixedThreadPool(5));

    private final PurgeDao purgeDao;

    public void launchPurge() {
        log.info("------ Purge start ------");
        try {
            var now = now().atStartOfDay().atZone(systemDefault()).toInstant();
            var instances = purgeDao.getInstances();
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
                    var stringIds = ids.stream().collect(joining("','", "'", "'"));
                    tasks.add(purge(stringIds, dateLimit, instance.getEnv(), instance.getName()));
                }
            }
            allOf(tasks.toArray(new CompletableFuture[0])).join();
        }
        finally {
            log.info("------ Purge finally ------");
            purge().join();
            log.info("------ Purge vacuum ------");
            purgeDao.vacuumAnalyze();
        }
        log.info("------ Purge end ------");
    }

    public CompletableFuture<Void> purge(String ids, Timestamp dateLimit, String env, String app) {
        return allOf( 
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("ins_trc", ids, dateLimit, false), "InstanceTrace", app, env, dateLimit), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("rsc_usg", ids, dateLimit, false), "ResourceUsage", app, env, dateLimit), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeSessionStage("rst_ses", "rst_ses_stg", ids, dateLimit), "RestSession", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("rst_ses", ids, dateLimit, true), "RestSessionStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequestStage("rst_rqt", "rst_rqt_stg", REST.name(), ids, dateLimit), "RestRequest", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("rst_rqt", ids, dateLimit, true), "RestRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequestStage("smtp_rqt", "smtp_stg", SMTP.name(), ids, dateLimit), "SmtpRequest", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("smtp_rqt", ids, dateLimit, true), "SmtpRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequestStage("ftp_rqt", "ftp_stg", FTP.name(), ids, dateLimit), "FtpRequest", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("ftp_rqt", ids, dateLimit, true), "FtpRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequestStage("ldap_rqt", "ldap_stg", LDAP.name(), ids, dateLimit), "LdapRequest", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("ldap_rqt", ids, dateLimit, true), "LdapRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequestStage("dtb_rqt", "dtb_stg", JDBC.name(), ids, dateLimit), "JdbcRequest", app, env, dateLimit), technicalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequest("dtb_rqt", ids, dateLimit, true), "JdbcRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("lcl_rqt", ids, dateLimit, true), "LclRequestStage", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("main_ses", ids, dateLimit, true), "MainSession", app, env, dateLimit), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("log_ent", ids, dateLimit, false), "LogEntry", app, env, dateLimit), functionalExecutor)
        );
    }

    public CompletableFuture<Void> purge() {
        return allOf(
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("log_ent"), "LogEntry"), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("lcl_rqt"), "LocalRequest"), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("main_ses"), "MainSession"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeSessionStage("usr_acn", "main_ses"), "UserAction"), functionalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("rst_ses"), "RestSession"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeSessionStage("rst_ses_stg", "rst_ses"), "RestSessionStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("rst_rqt"), "RestRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequestStage("rst_rqt_stg", "rst_rqt", REST.name()), "RestRequestStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("smtp_rqt"), "SmtpRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequestStage("smtp_stg", "smtp_rqt", SMTP.name()), "SmtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("ftp_rqt"), "FtpRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequestStage("ftp_stg", "ftp_rqt", FTP.name()), "FtpRequestStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("ldap_rqt"), "LdapRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequestStage("ldap_stg", "ldap_rqt", LDAP.name()), "LdapRequestStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("dtb_rqt"), "JdbcRequest"), functionalExecutor)
                        .thenRunAsync(runnablePurge(() -> purgeDao.purgeRequestStage("dtb_stg", "dtb_rqt", JDBC.name()), "JdbcRequestStage"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("ins_trc"), "InstanceTrace"), technicalExecutor),
                runAsync(runnablePurge(() -> purgeDao.purgeRequest("rsc_usg"), "ResourceUsage"), technicalExecutor)
        );
    }

    private Runnable runnablePurge(Supplier<Integer> action, String label, String app, String env, Timestamp dateLimit) {
        return () -> {
            var sum = action.get();
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

    private Runnable runnablePurge(Supplier<Integer> action, String label) {
        return runnablePurge(action, label, null, null, null);
    }
}
