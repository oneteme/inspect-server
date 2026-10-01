package org.usf.inspect.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.server.dao.PurgeDaoTestIds.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
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
class PurgeDaoByInstanceIdsH2Test {

    @Autowired
    private PurgeDao purgeDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purgeMainSession_ShouldDeleteOnlyExpiredSelectedSessions() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeMainSession(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_main_ses", "id_ses", OLD_MAIN_SESSION_ID));
        assertEquals(1, countById("e_main_ses", "id_ses", RECENT_MAIN_SESSION_ID));
        assertEquals(1, countById("e_main_ses", "id_ses", OTHER_MAIN_SESSION_ID));
    }

    @Test
    void purgeRestSession_ShouldDeleteOnlyExpiredSelectedSessions() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeRestSession(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_rst_ses", "id_ses", OLD_REST_SESSION_ID));
        assertEquals(1, countById("e_rst_ses", "id_ses", RECENT_REST_SESSION_ID));
        assertEquals(1, countById("e_rst_ses", "id_ses", OTHER_REST_SESSION_ID));
    }

    @Test
    void purgeLocalRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeLocalRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_lcl_rqt", "id_lcl_rqt", OLD_LOCAL_REQUEST_ID));
        assertEquals(1, countById("e_lcl_rqt", "id_lcl_rqt", RECENT_LOCAL_REQUEST_ID));
        assertEquals(1, countById("e_lcl_rqt", "id_lcl_rqt", OTHER_LOCAL_REQUEST_ID));
    }

    @Test
    void purgeDtbRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeDtbRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_dtb_rqt", "id_dtb_rqt", OLD_DATABASE_REQUEST_ID));
        assertEquals(1, countById("e_dtb_rqt", "id_dtb_rqt", RECENT_DATABASE_REQUEST_ID));
        assertEquals(1, countById("e_dtb_rqt", "id_dtb_rqt", OTHER_DATABASE_REQUEST_ID));
    }

    @Test
    void purgeLdapRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeLdapRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_ldap_rqt", "id_ldap_rqt", OLD_LDAP_REQUEST_ID));
        assertEquals(1, countById("e_ldap_rqt", "id_ldap_rqt", RECENT_LDAP_REQUEST_ID));
        assertEquals(1, countById("e_ldap_rqt", "id_ldap_rqt", OTHER_LDAP_REQUEST_ID));
    }

    @Test
    void purgeFtpRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeFtpRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_ftp_rqt", "id_ftp_rqt", OLD_FTP_REQUEST_ID));
        assertEquals(1, countById("e_ftp_rqt", "id_ftp_rqt", RECENT_FTP_REQUEST_ID));
        assertEquals(1, countById("e_ftp_rqt", "id_ftp_rqt", OTHER_FTP_REQUEST_ID));
    }

    @Test
    void purgeSmtpRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeSmtpRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_smtp_rqt", "id_smtp_rqt", OLD_SMTP_REQUEST_ID));
        assertEquals(1, countById("e_smtp_rqt", "id_smtp_rqt", RECENT_SMTP_REQUEST_ID));
        assertEquals(1, countById("e_smtp_rqt", "id_smtp_rqt", OTHER_SMTP_REQUEST_ID));
    }

    @Test
    void purgeRestRequest_ShouldDeleteOnlyExpiredSelectedRequests() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeRestRequest(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(0, countById("e_rst_rqt", "id_rst_rqt", OLD_HTTP_REQUEST_ID));
        assertEquals(1, countById("e_rst_rqt", "id_rst_rqt", RECENT_HTTP_REQUEST_ID));
        assertEquals(1, countById("e_rst_rqt", "id_rst_rqt", OTHER_HTTP_REQUEST_ID));
    }

    @Test
    void purgeInstanceTrace_ShouldDeleteOnlyExpiredSelectedTraces() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeInstanceTrace(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(1, countByInstance("e_ins_trc", TARGET_INSTANCE_ID));
        assertEquals(1, countByInstance("e_ins_trc", OTHER_INSTANCE_ID));
    }

    @Test
    void purgeLogEntry_ShouldDeleteOnlyExpiredSelectedLogs() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeLogEntry(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(1, countByInstance("e_log_ent", TARGET_INSTANCE_ID));
        assertEquals(1, countByInstance("e_log_ent", OTHER_INSTANCE_ID));
    }

    @Test
    void purgeResourceUsage_ShouldDeleteOnlyExpiredSelectedUsages() {
        var before = timestampDaysAgo(7);

        var deletedRows = purgeDao.purgeResourceUsage(List.of(TARGET_INSTANCE_ID), before);

        assertEquals(1, deletedRows);
        assertEquals(1, countByInstance("e_rsc_usg", TARGET_INSTANCE_ID));
        assertEquals(1, countByInstance("e_rsc_usg", OTHER_INSTANCE_ID));
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
}
