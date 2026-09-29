package org.usf.inspect.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.Retention;
import org.usf.inspect.server.dao.PurgeDao;

import static org.mockito.Mockito.doReturn;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/h2/purge-test-db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql",
        "inspect.server.purge.enabled=false",
        "inspect.server.partition.enabled=false",
        "inspect.collector.enabled=false",
        "inspect.server.scheduling.interval=365d",
        "inspect.collector.scheduling.interval=365d"
})
class PurgeServiceH2Test {

    private static final UUID ACTIVE_INSTANCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLOSED_INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ABANDONED_INSTANCE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ABANDONED_INSTANCE2_ID = UUID.fromString("99999999-3333-3333-3333-333333333333");
    private static final UUID ABANDONED_INSTANCE3_ID = UUID.fromString("77777777-3333-3333-3333-333333333333");
    private static final UUID ABANDONED_INSTANCE4_ID = UUID.fromString("88888888-3333-3333-3333-333333333333");
    private static final UUID ABANDONED_INSTANCE5_ID = UUID.fromString("00000000-3333-3333-3333-333333333333");
    private static final UUID ACTIVE_REQUEST_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID CLOSED_REQUEST_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ABANDONED_REQUEST_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ABANDONED_REQUEST2_ID = UUID.fromString("99999999-6666-6666-6666-666666666666");
    private static final UUID ABANDONED_REQUEST3_ID = UUID.fromString("77777777-6666-6666-6666-666666666666");
    private static final UUID ABANDONED_REQUEST4_ID = UUID.fromString("88888888-6666-6666-6666-666666666666");
    private static final UUID ABANDONED_REQUEST5_ID = UUID.fromString("00000000-6666-6666-6666-666666666666");

    @Autowired
    private PurgeService purgeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private PurgeDao purgeDao;

    @Test
    void dataArePersistedInH2BeforePurge() {
        cleanTables();
       Timestamp now = Timestamp.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
       Timestamp oldAuditDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(10)));
       Timestamp oldDiagnosticDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(12)));
       Timestamp abandonedRecentTraceDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(8)));
       Timestamp activeOldTraceDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(15)));
       Timestamp recentDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(1)));

       insertScenarioData(oldAuditDate, oldDiagnosticDate, abandonedRecentTraceDate, activeOldTraceDate, recentDate);

       assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_env_ins", Integer.class));
       assertEquals(12, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_ins_trc", Integer.class));
       assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_log_ent", Integer.class));
       assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt", Integer.class));
       assertEquals(7, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg", Integer.class));
    }

    @Test
    void launchPurge_ShouldDeleteExpiredDataAndKeepRecentData() {
       cleanTables();

       Timestamp now = Timestamp.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
       Timestamp oldAuditDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(10)));
       Timestamp oldDiagnosticDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(12)));
       Timestamp abandonedRecentTraceDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(8)));
       Timestamp activeOldTraceDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(15)));
       Timestamp recentDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(1)));
       Retention retention = new Retention(Duration.ofDays(10), Duration.ofDays(7));

       doReturn(List.of(new PurgeDao.PurgeScope(InstanceType.SERVER, "app-a","JARVIS_DEV", retention)))
              .when(purgeDao).selectInstances();

       insertScenarioData(oldAuditDate, oldDiagnosticDate, abandonedRecentTraceDate, activeOldTraceDate, recentDate);

      purgeService.launchPurge();

       assertEquals(1, count("e_env_ins", ACTIVE_INSTANCE_ID));
       assertEquals(0, count("e_env_ins", CLOSED_INSTANCE_ID));
       assertEquals(0, count("e_env_ins", ABANDONED_INSTANCE_ID));

       assertEquals(2, countInstanceTrace("e_ins_trc", ACTIVE_INSTANCE_ID));
       assertEquals(0, countInstanceTrace("e_ins_trc", CLOSED_INSTANCE_ID));
       assertEquals(0, countInstanceTrace("e_ins_trc", ABANDONED_INSTANCE_ID));

       assertEquals(1, countInstanceTrace("e_log_ent", ACTIVE_INSTANCE_ID));
       assertEquals(0, countInstanceTrace("e_log_ent", CLOSED_INSTANCE_ID));
       assertEquals(0, countInstanceTrace("e_log_ent", ABANDONED_INSTANCE_ID));

       assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt WHERE id_rst_rqt = ?", Integer.class, ACTIVE_REQUEST_ID));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt WHERE id_rst_rqt = ?", Integer.class, CLOSED_REQUEST_ID));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt WHERE id_rst_rqt = ?", Integer.class, ABANDONED_REQUEST_ID));
       assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg WHERE cd_rst_rqt = ?", Integer.class, ACTIVE_REQUEST_ID));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg WHERE cd_rst_rqt = ?", Integer.class, CLOSED_REQUEST_ID));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg WHERE cd_rst_rqt = ?", Integer.class, ABANDONED_REQUEST_ID));



    }

    @Test
    void launchPurge_ShouldFallbackToInstanceStartDate_WhenNoInstanceTraceExists() {
       cleanTables();

       Timestamp now = Timestamp.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
       Timestamp oldAuditDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(10)));
       Timestamp oldDiagnosticDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(12)));
       Timestamp activeOldTraceDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(15)));
       Timestamp recentDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(1)));
       Retention retention = new Retention(Duration.ofDays(10), Duration.ofDays(7));
       UUID noTraceInstanceId = UUID.fromString("77777777-7777-7777-7777-777777777777");
       UUID noTraceRequestId = UUID.fromString("88888888-8888-8888-8888-888888888888");
       UUID recentNoTraceInstanceId = UUID.fromString("99999999-9999-9999-9999-999999999999");
       UUID recentNoTraceRequestId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

       doReturn(List.of(new PurgeDao.PurgeScope(InstanceType.SERVER, "dev", "app-a","JARVIS_DEV", retention)))
              .when(purgeDao).selectInstances();

       insertInstance(CLOSED_INSTANCE_ID, oldAuditDate, oldAuditDate, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_DEV");
       insertInstance(noTraceInstanceId, oldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_TEST");
       insertInstance(recentNoTraceInstanceId, recentDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_TEST");
       insertInstance(ACTIVE_INSTANCE_ID, recentDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_REC");

       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
              0, 0, 0, 1, oldDiagnosticDate, CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
              0, 0, 0, 1, activeOldTraceDate, ACTIVE_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
              0, 0, 0, 1, recentDate, ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
              oldDiagnosticDate, "INFO", "closed", CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
              oldDiagnosticDate, "INFO", "no-trace", noTraceInstanceId);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
              recentDate, "INFO", "recent-no-trace", recentNoTraceInstanceId);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
              recentDate, "INFO", "active", ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
              CLOSED_REQUEST_ID, oldAuditDate, oldAuditDate, CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
              noTraceRequestId, oldAuditDate, oldAuditDate, noTraceInstanceId);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
              recentNoTraceRequestId, recentDate, recentDate, recentNoTraceInstanceId);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
              ACTIVE_REQUEST_ID, recentDate, recentDate, ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
              "closed-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, CLOSED_REQUEST_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
              "no-trace-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, noTraceRequestId);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
              "recent-no-trace-stage", recentDate, recentDate, 1L, recentNoTraceRequestId);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
              "active-stage", recentDate, recentDate, 1L, ACTIVE_REQUEST_ID);

       purgeService.launchPurge();

       assertEquals(0, count("e_env_ins", noTraceInstanceId));
       assertEquals(0, countInstanceTrace("e_log_ent", noTraceInstanceId));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt WHERE id_rst_rqt = ?", Integer.class, noTraceRequestId));
       assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg WHERE cd_rst_rqt = ?", Integer.class, noTraceRequestId));
       assertEquals(1, count("e_env_ins", recentNoTraceInstanceId));
       assertEquals(1, countInstanceTrace("e_log_ent", recentNoTraceInstanceId));
       assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt WHERE id_rst_rqt = ?", Integer.class, recentNoTraceRequestId));
       assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e_rst_rqt_stg WHERE cd_rst_rqt = ?", Integer.class, recentNoTraceRequestId));
       assertEquals(1, count("e_env_ins", ACTIVE_INSTANCE_ID));
       assertEquals(2, countInstanceTrace("e_ins_trc", ACTIVE_INSTANCE_ID));
    }

    private void insertScenarioData(Timestamp oldAuditDate, Timestamp oldDiagnosticDate, Timestamp abandonedRecentTraceDate, Timestamp activeOldTraceDate, Timestamp recentDate) {
        Timestamp now = Timestamp.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp notOldAuditDate = Timestamp.from(now.toInstant().minus(Duration.ofDays(2)));
       insertInstance(CLOSED_INSTANCE_ID, oldAuditDate, oldAuditDate, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_ISI");
       insertInstance(ABANDONED_INSTANCE_ID, oldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_DEV");
       insertInstance(ABANDONED_INSTANCE2_ID, notOldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_DEV");
       insertInstance(ACTIVE_INSTANCE_ID, recentDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_REC");

        insertInstance(ABANDONED_INSTANCE3_ID, oldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_VAL");
        insertInstance(ABANDONED_INSTANCE4_ID, notOldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_VAL");
        insertInstance(ABANDONED_INSTANCE5_ID, notOldAuditDate, null, retentionConfiguration(Duration.ofDays(10), Duration.ofDays(7)), "JARVIS_VAL");

       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, ABANDONED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, abandonedRecentTraceDate, ABANDONED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, ABANDONED_INSTANCE2_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, recentDate, ABANDONED_INSTANCE2_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, ABANDONED_INSTANCE3_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, abandonedRecentTraceDate, ABANDONED_INSTANCE3_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, ABANDONED_INSTANCE4_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, recentDate, ABANDONED_INSTANCE4_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, oldDiagnosticDate, ABANDONED_INSTANCE5_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, activeOldTraceDate, ACTIVE_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES (?, ?, ?, ?, ?, ?)",
               0, 0, 0, 1, recentDate, ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               oldDiagnosticDate, "INFO", "closed", CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               oldDiagnosticDate, "INFO", "abandoned", ABANDONED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               recentDate, "INFO", "abandoned-2", ABANDONED_INSTANCE2_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               oldDiagnosticDate, "INFO", "abandoned-3", ABANDONED_INSTANCE3_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               recentDate, "INFO", "abandoned-4", ABANDONED_INSTANCE4_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               oldDiagnosticDate, "INFO", "abandoned-5", ABANDONED_INSTANCE5_ID);
       jdbcTemplate.update("INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES (?, ?, ?, ?)",
               recentDate, "INFO", "active", ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               CLOSED_REQUEST_ID, oldAuditDate, oldAuditDate, CLOSED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ABANDONED_REQUEST_ID, oldAuditDate, oldAuditDate, ABANDONED_INSTANCE_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ABANDONED_REQUEST2_ID, recentDate, recentDate, ABANDONED_INSTANCE2_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ABANDONED_REQUEST3_ID, oldAuditDate, oldAuditDate, ABANDONED_INSTANCE3_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ABANDONED_REQUEST4_ID, recentDate, recentDate, ABANDONED_INSTANCE4_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ABANDONED_REQUEST5_ID, oldAuditDate, oldAuditDate, ABANDONED_INSTANCE5_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES (?, ?, ?, ?)",
               ACTIVE_REQUEST_ID, recentDate, recentDate, ACTIVE_INSTANCE_ID);

       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "closed-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, CLOSED_REQUEST_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "abandoned-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, ABANDONED_REQUEST_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "abandoned-2-stage", recentDate, recentDate, 1L, ABANDONED_REQUEST2_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "abandoned-3-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, ABANDONED_REQUEST3_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "abandoned-4-stage", recentDate, recentDate, 1L, ABANDONED_REQUEST4_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "abandoned-5-stage", oldDiagnosticDate, oldDiagnosticDate, 1L, ABANDONED_REQUEST5_ID);
       jdbcTemplate.update("INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES (?, ?, ?, ?, ?)",
               "active-stage", recentDate, recentDate, 1L, ACTIVE_REQUEST_ID);
    }

    private void insertInstance(UUID id, Timestamp start, Timestamp end, String configuration, String namespace) {
        jdbcTemplate.update("""
                INSERT INTO e_env_ins
                (id_ins, va_typ, dh_str, dh_end, va_app, va_env, va_cnf,cd_nsp)
                VALUES (?, ?, ?, ?, ?, ?, ?,?)
                """, id, "SERVER", start, end, "app-a", "dev", configuration, namespace);
    }

    private int count(String table, UUID instanceId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id_ins = ?", Integer.class, instanceId);
    }

    private int countInstanceTrace(String table, UUID instanceId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE cd_ins = ?", Integer.class, instanceId);
    }

    private void cleanTables() {
        jdbcTemplate.update("DELETE FROM e_rst_rqt_stg");
        jdbcTemplate.update("DELETE FROM e_rst_rqt");
        jdbcTemplate.update("DELETE FROM e_log_ent");
        jdbcTemplate.update("DELETE FROM e_ins_trc");
        jdbcTemplate.update("DELETE FROM e_env_ins");
    }

    private String retentionConfiguration(Duration audit, Duration diagnostic) {
        return """
                {"tracing":{"remote":{"retentionMaxAge":{"audit":"%s","diagnostic":"%s"}}}}
                """.formatted(audit, diagnostic);
    }
}
