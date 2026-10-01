package org.usf.inspect.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.core.TraceHub.hub;
import static org.usf.inspect.server.service.PurgeServiceTestData.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.server.dao.PurgeDao;

@SpringBootTest
@ActiveProfiles("test")
class PurgeServiceStepsH2Test {

    @Autowired
    private PurgeService purgeService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PurgeDao purgeDao;

    @BeforeEach
    void cleanDatabase() {
        var configuration = new InspectCollectorConfiguration();
        configuration.setEnabled(false);
        ((TraceDispatcherHub) hub()).configure(configuration);
        executeSql("cleanup.sql");
    }

    @Test
    void selectInstanceIds_ShouldReturnOnlyExpiredOpenInstancesInScope() {
        executeSql("jdd-insert-purge-service-select-instance-ids-test.sql");

        var ids = purgeDao.selectInstanceIds(
                timestampDaysAgo(7),
                NAMESPACE,
                APP,
                InstanceType.SERVER
        );

        assertEquals(List.of(OLD_INSTANCE_ID), ids);
    }

    @Test
    void purgeInstance_ShouldDeleteOnlyExpiredClosedInstances() {
        executeSql("jdd-insert-purge-service-instance-test.sql");

        var deletedRows = purgeDao.purgeInstance("dev", APP, timestampDaysAgo(7));

        assertEquals(1, deletedRows);
        assertFalse(instanceExists(OLD_INSTANCE_ID));
        assertTrue(instanceExists(RECENT_INSTANCE_ID));
    }

    @Test
    void purge_ShouldPurgeExpiredDataForSelectedInstanceIds() {
        executeSql("jdd-insert-purge-service-scoped-test.sql");

        CompletableFuture<Void> purge = ReflectionTestUtils.invokeMethod(
                purgeService,
                "purge",
                List.of(OLD_INSTANCE_ID),
                timestampDaysAgo(10),
                timestampDaysAgo(7),
                NAMESPACE,
                APP
        );
        purge.join();

        assertEquals(1, countByInstance("e_ins_trc", OLD_INSTANCE_ID));
        assertFalse(requestExists(OLD_REQUEST_ID));
        assertTrue(requestExists(RECENT_REQUEST_ID));
    }

    @Test
    void purgeAbandonedInstances_ShouldUseLatestTraceOrInstanceStartDate() {
        executeSql("jdd-insert-purge-service-abandoned-instances-test.sql");

        var deletedRows = purgeDao.purgeAbandonedInstances(
                NAMESPACE,
                APP,
                timestampDaysAgo(7)
        );

        assertEquals(2, deletedRows);
        assertFalse(instanceExists(OLD_INSTANCE_ID));
        assertTrue(instanceExists(RECENT_INSTANCE_ID));
        assertFalse(instanceExists(OTHER_INSTANCE_ID));
    }

    @Test
    void purge_ShouldDeleteExpiredOrphanData() {
        executeSql("jdd-insert-purge-service-orphan-data-test.sql");

        CompletableFuture<Void> purge = ReflectionTestUtils.invokeMethod(purgeService, "purge");
        purge.join();

        assertEquals(0, countByInstance("e_ins_trc", ORPHAN_INSTANCE_ID));
        assertEquals(0, countByInstance("e_log_ent", ORPHAN_INSTANCE_ID));
    }

    private boolean instanceExists(UUID id) {
        return countById("e_env_ins", "id_ins", id) == 1;
    }

    private boolean requestExists(UUID id) {
        return countById("e_rst_rqt", "id_rst_rqt", id) == 1;
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

    private Timestamp timestampDaysAgo(long days) {
        return Timestamp.valueOf(LocalDate.now().minusDays(days).atStartOfDay());
    }

    private void executeSql(String fileName) {
        var script = new ClassPathResource("data/purge-service/" + fileName);
        new ResourceDatabasePopulator(script).execute(jdbcTemplate.getDataSource());
    }
}
