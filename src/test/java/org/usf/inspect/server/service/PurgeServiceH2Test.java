package org.usf.inspect.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.server.service.PurgeServiceTestData.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.Retention;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.server.dao.PurgeDao;

@SpringBootTest
@ActiveProfiles("test")
class PurgeServiceH2Test {

    @Autowired
    private PurgeService purgeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private PurgeDao purgeDao;

    @BeforeEach
    void cleanDatabase() {
        var configuration = new InspectCollectorConfiguration();
        configuration.setEnabled(false);
        ((TraceDispatcherHub) hub()).configure(configuration);
        executeSql("cleanup.sql");
    }

    @Test
    void dataArePersistedInH2BeforePurge() {
        executeSql("jdd-insert-purge-service-launch-test.sql");

        assertEquals(7, countRows("e_env_ins"));
        assertEquals(12, countRows("e_ins_trc"));
        assertEquals(7, countRows("e_log_ent"));
        assertEquals(7, countRows("e_rst_rqt"));
        assertEquals(7, countRows("e_rst_rqt_stg"));
    }

    @Test
    void launchPurge_ShouldDeleteExpiredDataAndKeepRecentData() {
        executeSql("jdd-insert-purge-service-launch-test.sql");
        mockPurgeScope();

        purgeService.launchPurge();

        assertEquals(1, countInstance(ACTIVE_INSTANCE_ID));
        assertEquals(0, countInstance(CLOSED_INSTANCE_ID));
        assertEquals(0, countInstance(ABANDONED_INSTANCE_ID));

        assertEquals(2, countByInstance("e_ins_trc", ACTIVE_INSTANCE_ID));
        assertEquals(0, countByInstance("e_ins_trc", CLOSED_INSTANCE_ID));
        assertEquals(0, countByInstance("e_ins_trc", ABANDONED_INSTANCE_ID));

        assertEquals(1, countByInstance("e_log_ent", ACTIVE_INSTANCE_ID));
        assertEquals(0, countByInstance("e_log_ent", CLOSED_INSTANCE_ID));
        assertEquals(0, countByInstance("e_log_ent", ABANDONED_INSTANCE_ID));

        assertEquals(1, countById("e_rst_rqt", "id_rst_rqt", ACTIVE_REQUEST_ID));
        assertEquals(0, countById("e_rst_rqt", "id_rst_rqt", CLOSED_REQUEST_ID));
        assertEquals(0, countById("e_rst_rqt", "id_rst_rqt", ABANDONED_REQUEST_ID));
        assertEquals(1, countById("e_rst_rqt_stg", "cd_rst_rqt", ACTIVE_REQUEST_ID));
        assertEquals(0, countById("e_rst_rqt_stg", "cd_rst_rqt", CLOSED_REQUEST_ID));
        assertEquals(0, countById("e_rst_rqt_stg", "cd_rst_rqt", ABANDONED_REQUEST_ID));
    }

    @Test
    void launchPurge_ShouldFallbackToInstanceStartDate_WhenNoInstanceTraceExists() {
        executeSql("jdd-insert-purge-service-fallback-test.sql");
        mockPurgeScope();

        purgeService.launchPurge();

        assertEquals(0, countInstance(NO_TRACE_INSTANCE_ID));
        assertEquals(0, countByInstance("e_log_ent", NO_TRACE_INSTANCE_ID));
        assertEquals(0, countById("e_rst_rqt", "id_rst_rqt", NO_TRACE_REQUEST_ID));
        assertEquals(0, countById("e_rst_rqt_stg", "cd_rst_rqt", NO_TRACE_REQUEST_ID));
        assertEquals(1, countInstance(RECENT_NO_TRACE_INSTANCE_ID));
        assertEquals(1, countByInstance("e_log_ent", RECENT_NO_TRACE_INSTANCE_ID));
        assertEquals(1, countById("e_rst_rqt", "id_rst_rqt", RECENT_NO_TRACE_REQUEST_ID));
        assertEquals(1, countById("e_rst_rqt_stg", "cd_rst_rqt", RECENT_NO_TRACE_REQUEST_ID));
        assertEquals(1, countInstance(ACTIVE_INSTANCE_ID));
        assertEquals(2, countByInstance("e_ins_trc", ACTIVE_INSTANCE_ID));
    }

    private void mockPurgeScope() {
        var retention = new Retention(Duration.ofDays(10), Duration.ofDays(7));
        doReturn(List.of(new PurgeDao.PurgeScope(InstanceType.SERVER, "app-a", "JARVIS_DEV", retention)))
                .when(purgeDao).selectInstances();
    }

    private int countRows(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int countInstance(UUID instanceId) {
        return countById("e_env_ins", "id_ins", instanceId);
    }

    private int countByInstance(String table, UUID instanceId) {
        return countById(table, "cd_ins", instanceId);
    }

    private int countById(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                id
        );
    }

    private void executeSql(String fileName) {
        var script = new ClassPathResource("data/purge-service/" + fileName);
        new ResourceDatabasePopulator(script).execute(jdbcTemplate.getDataSource());
    }
}
