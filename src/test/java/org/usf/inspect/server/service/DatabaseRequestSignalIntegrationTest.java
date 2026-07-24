package org.usf.inspect.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.server.dao.TraceDao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://calamar.pgaas.enedis.fr:32077/trace-api",
        "spring.datasource.username=trace-api-user",
        "spring.datasource.password=r5YVbWkr3t649Xc",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
class DatabaseRequestSignalIntegrationTest {

    @Autowired
    private TraceDao traceDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("savePartialDatabaseRequests : doit insérer un lot sans doublon")
    void shouldSaveDatabaseRequestsInBatchSuccessfully() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        List<DatabaseRequestSignal> batch = List.of(
                createDbSignal(id1),
                createDbSignal(id2)
        );

        assertThatCode(() -> traceDao.savePartialDatabaseRequests(batch))
                .doesNotThrowAnyException();

        assertThat(checkExists(id1)).isTrue();
        assertThat(checkExists(id2)).isTrue();
    }

    @Test
    @DisplayName("savePartialDatabaseRequests : doit ignorer les doublons et insérer les éléments valides")
    void shouldSkipMultipleDuplicatesAndInsertAllValidItems() {
        String duplicateId1 = UUID.randomUUID().toString();
        String duplicateId2 = UUID.randomUUID().toString();

        insertDummyDatabaseRequest(duplicateId1);
        insertDummyDatabaseRequest(duplicateId2);

        String validId1 = UUID.randomUUID().toString();
        String validId2 = UUID.randomUUID().toString();
        String validId3 = UUID.randomUUID().toString();

        List<DatabaseRequestSignal> batchWithMultipleDuplicates = List.of(
                createDbSignal(duplicateId1),
                createDbSignal(validId1),
                createDbSignal(duplicateId2),
                createDbSignal(validId2),
                createDbSignal(validId3)
        );

        assertThatCode(() -> traceDao.savePartialDatabaseRequests(batchWithMultipleDuplicates))
                .doesNotThrowAnyException();

        assertThat(checkExists(validId1)).isTrue();
        assertThat(checkExists(validId2)).isTrue();
        assertThat(checkExists(validId3)).isTrue();
        assertThat(checkExists(duplicateId1)).isTrue();
        assertThat(checkExists(duplicateId2)).isTrue();
    }

    private void insertDummyDatabaseRequest(String id) {
        jdbcTemplate.update("""
            INSERT INTO e_dtb_rqt (
                id_dtb_rqt, va_she, va_hst, cd_prt, va_nam, va_sha, 
                dh_str, va_usr, va_thr, va_drv, va_prd_nam, va_prd_vrs, 
                cd_ins
            ) 
            VALUES (
                ?::uuid, 'postgresql', 'localhost', 5432, 'testdb', 'public',
                NOW(), 'test_user', 'main', '42.6.0', 'PostgreSQL', '15.12',
                ?::uuid
            )
        """, id, UUID.randomUUID().toString());
    }

    private boolean checkExists(String id) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT count(*) > 0 FROM e_dtb_rqt WHERE id_dtb_rqt = ?::uuid",
                Boolean.class,
                id
        );
        return Boolean.TRUE.equals(exists);
    }

    private DatabaseRequestSignal createDbSignal(String id) {
        DatabaseRequestSignal signal = new DatabaseRequestSignal(
                id,
                UUID.randomUUID().toString(),
                Instant.now(),
                Thread.currentThread().getName()
        );
        signal.setScheme("postgresql");
        signal.setHost("calamar.pgaas.enedis.fr");
        signal.setPort(32077);
        signal.setName("trace-api");
        signal.setSchema("public");
        return signal;
    }
}