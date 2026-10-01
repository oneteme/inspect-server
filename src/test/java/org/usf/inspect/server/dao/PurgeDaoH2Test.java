package org.usf.inspect.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.server.dao.PurgeDaoTestIds.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/data/purge-dao/purge-dao-data.sql")
class PurgeDaoH2Test {

    @Autowired
    private PurgeDao purgeDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purgeLdapRequest_ShouldDeleteOnlyOldOrphanRequests() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeLdapRequest(before);

        assertEquals(1, deletedRows);
        assertEquals(2, countOldByDhStr("e_ldap_rqt", timestampDaysAgo(7)));
        assertEquals(2, countNewByDhStr("e_ldap_rqt", timestampDaysAgo(7)));
    }

    @Test
    void purgeLdapRequestStage_ShouldDeleteOnlyOrphanStages() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeLdapRequestStage(before);

        assertEquals(1, deletedRows);
        assertEquals(1, countById("e_ldap_stg", "cd_ldap_rqt", LINKED_LDAP_REQUEST_ID));
        assertEquals(0, countById("e_ldap_stg", "cd_ldap_rqt", ORPHAN_LDAP_REQUEST_ID));
    }

    @Test
    void purgeFtpRequest_ShouldDeleteOnlyOldOrphanRequests() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeFtpRequest(before);

        assertEquals(1, deletedRows);
        assertEquals(2, countOldByDhStr("e_ftp_rqt", timestampDaysAgo(7)));
        assertEquals(2, countNewByDhStr("e_ftp_rqt", timestampDaysAgo(7)));
    }

    @Test
    void purgeFtpRequestStage_ShouldDeleteOnlyOrphanStages() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeFtpRequestStage(before);

        assertEquals(1, deletedRows);
        assertEquals(1, countById("e_ftp_stg", "cd_ftp_rqt", LINKED_FTP_REQUEST_ID));
        assertEquals(0, countById("e_ftp_stg", "cd_ftp_rqt", ORPHAN_FTP_REQUEST_ID));
    }

    @Test
    void purgeSmtpRequest_ShouldDeleteOnlyOldOrphanRequests() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeSmtpRequest(before);

        assertEquals(1, deletedRows);
        assertEquals(2, countOldByDhStr("e_smtp_rqt", timestampDaysAgo(7)));
        assertEquals(2, countNewByDhStr("e_smtp_rqt", timestampDaysAgo(7)));
    }

    @Test
    void purgeSmtpRequestStage_ShouldDeleteOnlyOrphanStages() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeSmtpRequestStage(before);

        assertEquals(1, deletedRows);
        assertEquals(1, countById("e_smtp_stg", "cd_smtp_rqt", LINKED_SMTP_REQUEST_ID));
        assertEquals(0, countById("e_smtp_stg", "cd_smtp_rqt", ORPHAN_SMTP_REQUEST_ID));
    }



    @Test
    void purgeMainSession_ShouldDeleteOnlyOldOrphanSessions() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeMainSession(before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_main_ses", "id_ses", OLD_ORPHAN_MAIN_SESSION_ID));
        assertEquals(1, countById("e_main_ses", "id_ses", RECENT_ORPHAN_MAIN_SESSION_ID));
        assertEquals(1, countById("e_main_ses", "id_ses", LINKED_MAIN_SESSION_ID));
    }

    @Test
    void purgeRestSession_ShouldDeleteOnlyOldOrphanSessions() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeRestSession(before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_rst_ses", "id_ses", OLD_ORPHAN_REST_SESSION_ID));
        assertEquals(1, countById("e_rst_ses", "id_ses", RECENT_ORPHAN_REST_SESSION_ID));
        assertEquals(1, countById("e_rst_ses", "id_ses", LINKED_REST_SESSION_ID));
    }

    @Test
    void purgeException_ShouldDeleteOnlyOrphanExceptions() {
        var deletedRows = purgeDao.purgeException();

        assertEquals(1, deletedRows);
        assertEquals(1, countById("e_exc_inf", "cd_trc", LINKED_REST_REQUEST_ID));
        assertEquals(0, countById("e_exc_inf", "cd_trc", ORPHAN_REST_REQUEST_ID));
    }

    @Test
    void purgeInstanceTrace_ShouldDeleteOnlyOldOrphanTraces() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeInstanceTrace(before);

        assertEquals(1, deletedRows);
        assertEquals(0, countByInstance("e_ins_trc", OLD_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_ins_trc", RECENT_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_ins_trc", LINKED_INSTANCE_ID));
    }

    @Test
    void purgeResourceUsage_ShouldDeleteOnlyOldOrphanUsages() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeResourceUsage(before);

        assertEquals(1, deletedRows);
        assertEquals(0, countByInstance("e_rsc_usg", OLD_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_rsc_usg", RECENT_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_rsc_usg", LINKED_INSTANCE_ID));
    }

    @Test
    void purgeLogEntry_ShouldDeleteOnlyOldOrphanLogs() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeLogEntry(before);

        assertEquals(1, deletedRows);
        assertEquals(0, countByInstance("e_log_ent", OLD_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_log_ent", RECENT_ORPHAN_INSTANCE_ID));
        assertEquals(1, countByInstance("e_log_ent", LINKED_INSTANCE_ID));
    }

    @Test
    void purgeSessionEvent_ShouldDeleteOnlyOldOrphanEvents() {
        var before = LocalDate.now().minusDays(7);

        var deletedRows = purgeDao.purgeSessionEvent(before);

        assertEquals(1, deletedRows);
        assertEquals(2, countOldByDhStr("e_ses_evt", timestampDaysAgo(7)));
        assertEquals(1, countNewByDhStr("e_ses_evt", timestampDaysAgo(7)));
    }

    @Test
    void purgeBrowserConfig_ShouldDeleteOnlyOrphanConfigs() {
        var deletedRows = purgeDao.purgeBrowserConfig();

        assertEquals(1, deletedRows);
        assertEquals(1, countById("o_brw_cfg", "cd_prn_ses", LINKED_INSTANCE_ID));
        assertEquals(0, countById("o_brw_cfg", "cd_prn_ses", ORPHAN_BROWSER_CONFIG_ID));
    }

    private int countById(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id
        );
    }

    private int countByInstance(String table, UUID instanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE cd_ins = ?",
                Integer.class,
                instanceId
        );
    }

    private Timestamp timestampDaysAgo(long days) {
        return Timestamp.valueOf(LocalDate.now().minusDays(days).atStartOfDay());
    }

    private int countOldByDhStr(String table, Timestamp dh_str) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE dh_str < ?",
                Integer.class,
                dh_str
        );
    }

    private int countNewByDhStr(String table, Timestamp dh_str) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE dh_str > ?",
                Integer.class,
                dh_str
        );
    }
}
