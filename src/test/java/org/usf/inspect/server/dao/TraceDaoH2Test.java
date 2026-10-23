package org.usf.inspect.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.server.JsonUtils.defaultMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.DatabaseRequestSignal;
import org.usf.inspect.DatabaseRequestStage;
import org.usf.inspect.DatabaseRequestUpdate;
import org.usf.inspect.DirectoryRequestSignal;
import org.usf.inspect.DirectoryRequestStage;
import org.usf.inspect.DirectoryRequestUpdate;
import org.usf.inspect.ExceptionTrace;
import org.usf.inspect.FtpRequestSignal;
import org.usf.inspect.FtpRequestStage;
import org.usf.inspect.FtpRequestUpdate;
import org.usf.inspect.HttpRequestSignal;
import org.usf.inspect.HttpRequestStage;
import org.usf.inspect.HttpRequestUpdate;
import org.usf.inspect.HttpSessionSignal;
import org.usf.inspect.HttpSessionStage;
import org.usf.inspect.HttpSessionUpdate;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.InstanceType;
import org.usf.inspect.LocalRequestSignal;
import org.usf.inspect.LocalRequestUpdate;
import org.usf.inspect.MailRequestSignal;
import org.usf.inspect.MailRequestStage;
import org.usf.inspect.MailRequestUpdate;
import org.usf.inspect.MainSessionSignal;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.ReportEvent;
import org.usf.inspect.SessionEvent;
import org.usf.inspect.SessionMaskUpdate;
import org.usf.inspect.StackTraceRow;
import org.usf.inspect.SystemMetrics;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dto.BrowserConfigDto;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.Pair;
import org.usf.inspect.server.model.TraceBatch;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceDaoH2Test {
    private static final UUID TRACE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID INSTANCE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");
    private static final String SEED = "/data/trace-dao/jdd-insert-trace-dao-test.sql";

    @Autowired
    private TraceDao traceDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private tools.jackson.databind.JsonNode fixtures;

    @BeforeAll
    void loadFixtures() throws IOException {
        try (var input = getClass().getResourceAsStream("/data/trace-dao/traces.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing TraceDao fixtures");
            }
            fixtures = defaultMapper.readTree(input);
        }
    }

    @Nested
    class SaveInstanceEnvironment {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var environment = new InstanceEnvironment(INSTANCE_ID, START, InstanceType.SERVER,
                    "inspect-test", "1.5.0", "test", "192.0.2.10", "Windows", "Java 21",
                    "service", "main", "abc123", "spring-collector", Map.of("region", "eu"), null);

            traceDao.saveInstanceEnvironment(environment);

            assertSingleRow("e_env_ins", data("environment").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var environment = value(data("environment").required("input"), InstanceEnvironment.class);

            traceDao.saveInstanceEnvironment(environment);

            assertSingleRow("e_env_ins", data("environment").required("row"));
        }
    }

    @Nested
    class UpdateInstanceEnvironments {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() {
            var end = START.plusSeconds(5);
            var expected = row("e_env_ins", "id_ins", TRACE_ID);

            traceDao.updateInstanceEnvironments(List.of(new InstanceEnvironmentUpdate(TRACE_ID, end)));

            expected.put("dh_end", Timestamp.from(end));
            assertEquals(expected, row("e_env_ins", "id_ins", TRACE_ID));
        }

        @Test
        @Sql(SEED)
        void withAllFields() {
            var end = START.plusSeconds(10);
            var expected = row("e_env_ins", "id_ins", TRACE_ID);

            traceDao.updateInstanceEnvironments(List.of(new InstanceEnvironmentUpdate(TRACE_ID, end)));

            expected.put("dh_end", Timestamp.from(end));
            assertEquals(expected, row("e_env_ins", "id_ins", TRACE_ID));
        }
    }

    @Nested
    class SaveTracePackets {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var packet = new TraceBatch(START, 2, INSTANCE_ID, 12, 3, 7);

            traceDao.saveTracePackets(List.of(packet));

            assertSingleRow("e_ins_trc", data("packet").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var packet = value(data("packet").required("input"), TraceBatch.class);

            traceDao.saveTracePackets(List.of(packet));

            assertSingleRow("e_ins_trc", data("packet").required("row"));
        }
    }

    @Nested
    class SaveLogEntries {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var logEntry = new ReportEvent(START, "main", "REPORT", "Slow request",null,
                    new StackTraceRow[] { new StackTraceRow("example.Service", "run", 42) });

            traceDao.saveLogEntries(List.of(logEntry));

            assertSingleRow("e_rpt_evt", data("log").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var logEntry = value(data("log").required("input"), ReportEvent.class);

            traceDao.saveLogEntries(List.of(logEntry));

            assertSingleRow("e_rpt_evt", data("log").required("row"));
        }
    }

    @Nested
    class SaveMachineResourceUsages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var usage = new SystemMetrics(START, 128, 512, 1024, 8, 24, (short) 1250);
            usage.setInstanceId(INSTANCE_ID);

            traceDao.saveMachineResourceUsages(List.of(usage));

            assertSingleRow("e_rsc_usg", data("resource").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var usage = value(data("resource").required("input"), SystemMetrics.class);

            traceDao.saveMachineResourceUsages(List.of(usage));

            assertSingleRow("e_rsc_usg", data("resource").required("row"));
        }
    }

    @Nested
    class SaveRestSessionSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new HttpSessionSignal(TRACE_ID, START, "http-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveRestSessionSignals(List.of(signal));

            assertSingleRow("e_rst_ses", data("restSession").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("restSession").required("signal"), HttpSessionSignal.class);

            traceDao.saveRestSessionSignals(List.of(signal));

            assertSingleRow("e_rst_ses", data("restSession").required("signalRow"));
        }
    }

    @Nested
    class SaveRestSessions {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new HttpSessionSignal(TRACE_ID, START, "http-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new HttpSessionUpdate(TRACE_ID);

            traceDao.saveRestSessions(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_rst_ses", data("restSession").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("restSession").required("signal"), HttpSessionSignal.class);
            var update = value(data("restSession").required("update"), HttpSessionUpdate.class);

            traceDao.saveRestSessions(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_rst_ses", mergedRow("restSession", "updateRow"));
        }
    }

    @Nested
    class UpdateRestSessions {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new HttpSessionUpdate(TRACE_ID);

            traceDao.updateRestSessions(List.of(update));

            assertExactRow(row("e_rst_ses", "id_ses", TRACE_ID), mergedRow("restSession", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("restSession").required("update"), HttpSessionUpdate.class);

            traceDao.updateRestSessions(List.of(update));

            assertExactRow(row("e_rst_ses", "id_ses", TRACE_ID), mergedRow("restSession", "updateRow"));
        }
    }

    @Nested
    class UpdateMaskRestSessions {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() {
            var expected = row("e_rst_ses", "id_ses", TRACE_ID);
            var update = new SessionMaskUpdate(TRACE_ID, false, 1);

            traceDao.updateMaskRestSessions(List.of(update));

            expected.put("va_msk", 1);
            assertEquals(expected, row("e_rst_ses", "id_ses", TRACE_ID));
        }

        @Test
        @Sql(SEED)
        void withAllFields() {
            var expected = row("e_rst_ses", "id_ses", TRACE_ID);
            var update = new SessionMaskUpdate(TRACE_ID, false, 37);

            traceDao.updateMaskRestSessions(List.of(update));

            expected.put("va_msk", 37);
            assertEquals(expected, row("e_rst_ses", "id_ses", TRACE_ID));
        }
    }

    @Nested
    class SaveMainSessionSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new MainSessionSignal(TRACE_ID, START, "main-worker", "SCHEDULE");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveMainSessionSignals(List.of(signal));

            assertSingleRow("e_main_ses", data("mainSession").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("mainSession").required("signal"), MainSessionSignal.class);

            traceDao.saveMainSessionSignals(List.of(signal));

            assertSingleRow("e_main_ses", data("mainSession").required("signalRow"));
        }
    }

    @Nested
    class SaveMainSessions {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new MainSessionSignal(TRACE_ID, START, "main-worker", "SCHEDULE");
            signal.setInstanceId(INSTANCE_ID);
            var update = new MainSessionUpdate(TRACE_ID);

            traceDao.saveMainSessions(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_main_ses", data("mainSession").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("mainSession").required("signal"), MainSessionSignal.class);
            var update = value(data("mainSession").required("update"), MainSessionUpdate.class);

            traceDao.saveMainSessions(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_main_ses", mergedRow("mainSession", "updateRow"));
        }
    }

    @Nested
    class UpdateMainSessions {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new MainSessionUpdate(TRACE_ID);

            traceDao.updateMainSessions(List.of(update));

            assertExactRow(row("e_main_ses", "id_ses", TRACE_ID), mergedRow("mainSession", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("mainSession").required("update"), MainSessionUpdate.class);

            traceDao.updateMainSessions(List.of(update));

            assertExactRow(row("e_main_ses", "id_ses", TRACE_ID), mergedRow("mainSession", "updateRow"));
        }
    }

    @Nested
    class UpdateMaskMainSessions {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() {
            var expected = row("e_main_ses", "id_ses", TRACE_ID);
            var update = new SessionMaskUpdate(TRACE_ID, true, 1);

            traceDao.updateMaskMainSessions(List.of(update));

            expected.put("va_msk", 1);
            assertEquals(expected, row("e_main_ses", "id_ses", TRACE_ID));
        }

        @Test
        @Sql(SEED)
        void withAllFields() {
            var expected = row("e_main_ses", "id_ses", TRACE_ID);
            var update = new SessionMaskUpdate(TRACE_ID, true, 37);

            traceDao.updateMaskMainSessions(List.of(update));

            expected.put("va_msk", 37);
            assertEquals(expected, row("e_main_ses", "id_ses", TRACE_ID));
        }
    }

    @Nested
    class SaveRestRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new HttpRequestSignal(TRACE_ID, SESSION_ID, START, "rest-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveRestRequestSignals(List.of(signal));

            assertSingleRow("e_rst_rqt", data("rest").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("rest").required("signal"), HttpRequestSignal.class);

            traceDao.saveRestRequestSignals(List.of(signal));

            assertSingleRow("e_rst_rqt", data("rest").required("signalRow"));
        }
    }

    @Nested
    class SaveRestRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new HttpRequestSignal(TRACE_ID, SESSION_ID, START, "rest-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new HttpRequestUpdate(TRACE_ID);

            traceDao.saveRestRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_rst_rqt", data("rest").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("rest").required("signal"), HttpRequestSignal.class);
            var update = value(data("rest").required("update"), HttpRequestUpdate.class);

            traceDao.saveRestRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_rst_rqt", mergedRow("rest", "updateRow"));
        }
    }

    @Nested
    class UpdateRestRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new HttpRequestUpdate(TRACE_ID);

            traceDao.updateRestRequests(List.of(update));

            assertExactRow(row("e_rst_rqt", "id_rst_rqt", TRACE_ID), mergedRow("rest", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("rest").required("update"), HttpRequestUpdate.class);

            traceDao.updateRestRequests(List.of(update));

            assertExactRow(row("e_rst_rqt", "id_rst_rqt", TRACE_ID), mergedRow("rest", "updateRow"));
        }
    }

    @Nested
    class SaveLocalRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new LocalRequestSignal(TRACE_ID, SESSION_ID, START, "local-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveLocalRequestSignals(List.of(signal));

            assertSingleRow("e_lcl_rqt", data("local").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("local").required("signal"), LocalRequestSignal.class);

            traceDao.saveLocalRequestSignals(List.of(signal));

            assertSingleRow("e_lcl_rqt", data("local").required("signalRow"));
        }
    }

    @Nested
    class SaveLocalRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new LocalRequestSignal(TRACE_ID, SESSION_ID, START, "local-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new LocalRequestUpdate(TRACE_ID);

            traceDao.saveLocalRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_lcl_rqt", data("local").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("local").required("signal"), LocalRequestSignal.class);
            var update = value(data("local").required("update"), LocalRequestUpdate.class);

            traceDao.saveLocalRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_lcl_rqt", mergedRow("local", "updateRow"));
        }
    }

    @Nested
    class UpdateLocalRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new LocalRequestUpdate(TRACE_ID);

            traceDao.updateLocalRequests(List.of(update));

            assertExactRow(row("e_lcl_rqt", "id_lcl_rqt", TRACE_ID), mergedRow("local", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("local").required("update"), LocalRequestUpdate.class);

            traceDao.updateLocalRequests(List.of(update));

            assertExactRow(row("e_lcl_rqt", "id_lcl_rqt", TRACE_ID), mergedRow("local", "updateRow"));
        }
    }

    @Nested
    class SaveMailRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new MailRequestSignal(TRACE_ID, SESSION_ID, START, "mail-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveMailRequestSignals(List.of(signal));

            assertSingleRow("e_smtp_rqt", data("mail").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("mail").required("signal"), MailRequestSignal.class);

            traceDao.saveMailRequestSignals(List.of(signal));

            assertSingleRow("e_smtp_rqt", data("mail").required("signalRow"));
        }
    }

    @Nested
    class SaveMailRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new MailRequestSignal(TRACE_ID, SESSION_ID, START, "mail-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new MailRequestUpdate(TRACE_ID);

            traceDao.saveMailRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_smtp_rqt", data("mail").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("mail").required("signal"), MailRequestSignal.class);
            var update = value(data("mail").required("update"), MailRequestUpdate.class);

            traceDao.saveMailRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_smtp_rqt", mergedRow("mail", "updateRow"));
        }
    }

    @Nested
    class UpdateMailRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new MailRequestUpdate(TRACE_ID);

            traceDao.updateMailRequests(List.of(update));

            assertExactRow(row("e_smtp_rqt", "id_smtp_rqt", TRACE_ID), mergedRow("mail", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("mail").required("update"), MailRequestUpdate.class);

            traceDao.updateMailRequests(List.of(update));

            assertExactRow(row("e_smtp_rqt", "id_smtp_rqt", TRACE_ID), mergedRow("mail", "updateRow"));
        }
    }

    @Nested
    class SaveFtpRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new FtpRequestSignal(TRACE_ID, SESSION_ID, START, "ftp-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveFtpRequestSignals(List.of(signal));

            assertSingleRow("e_ftp_rqt", data("ftp").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("ftp").required("signal"), FtpRequestSignal.class);

            traceDao.saveFtpRequestSignals(List.of(signal));

            assertSingleRow("e_ftp_rqt", data("ftp").required("signalRow"));
        }
    }

    @Nested
    class SaveFtpRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new FtpRequestSignal(TRACE_ID, SESSION_ID, START, "ftp-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new FtpRequestUpdate(TRACE_ID);

            traceDao.saveFtpRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_ftp_rqt", data("ftp").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("ftp").required("signal"), FtpRequestSignal.class);
            var update = value(data("ftp").required("update"), FtpRequestUpdate.class);

            traceDao.saveFtpRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_ftp_rqt", mergedRow("ftp", "updateRow"));
        }
    }

    @Nested
    class UpdateFtpRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new FtpRequestUpdate(TRACE_ID);

            traceDao.updateFtpRequests(List.of(update));

            assertExactRow(row("e_ftp_rqt", "id_ftp_rqt", TRACE_ID), mergedRow("ftp", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("ftp").required("update"), FtpRequestUpdate.class);

            traceDao.updateFtpRequests(List.of(update));

            assertExactRow(row("e_ftp_rqt", "id_ftp_rqt", TRACE_ID), mergedRow("ftp", "updateRow"));
        }
    }

    @Nested
    class SaveLdapRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new DirectoryRequestSignal(TRACE_ID, SESSION_ID, START, "ldap-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveLdapRequestSignals(List.of(signal));

            assertSingleRow("e_ldap_rqt", data("ldap").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("ldap").required("signal"), DirectoryRequestSignal.class);

            traceDao.saveLdapRequestSignals(List.of(signal));

            assertSingleRow("e_ldap_rqt", data("ldap").required("signalRow"));
        }
    }

    @Nested
    class SaveLdapRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new DirectoryRequestSignal(TRACE_ID, SESSION_ID, START, "ldap-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new DirectoryRequestUpdate(TRACE_ID);

            traceDao.saveLdapRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_ldap_rqt", data("ldap").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("ldap").required("signal"), DirectoryRequestSignal.class);
            var update = value(data("ldap").required("update"), DirectoryRequestUpdate.class);

            traceDao.saveLdapRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_ldap_rqt", mergedRow("ldap", "updateRow"));
        }
    }

    @Nested
    class UpdateLdapRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new DirectoryRequestUpdate(TRACE_ID);

            traceDao.updateLdapRequests(List.of(update));

            assertExactRow(row("e_ldap_rqt", "id_ldap_rqt", TRACE_ID), mergedRow("ldap", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("ldap").required("update"), DirectoryRequestUpdate.class);

            traceDao.updateLdapRequests(List.of(update));

            assertExactRow(row("e_ldap_rqt", "id_ldap_rqt", TRACE_ID), mergedRow("ldap", "updateRow"));
        }
    }

    @Nested
    class SaveDatabaseRequestSignals {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new DatabaseRequestSignal(TRACE_ID, SESSION_ID, START, "jdbc-worker");
            signal.setInstanceId(INSTANCE_ID);

            traceDao.saveDatabaseRequestSignals(List.of(signal));

            assertSingleRow("e_dtb_rqt", data("database").required("signalRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("database").required("signal"), DatabaseRequestSignal.class);

            traceDao.saveDatabaseRequestSignals(List.of(signal));

            assertSingleRow("e_dtb_rqt", data("database").required("signalRow"));
        }
    }

    @Nested
    class SaveDatabaseRequests {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var signal = new DatabaseRequestSignal(TRACE_ID, SESSION_ID, START, "jdbc-worker");
            signal.setInstanceId(INSTANCE_ID);
            var update = new DatabaseRequestUpdate(TRACE_ID);

            traceDao.saveDatabaseRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_dtb_rqt", data("database").required("pairRowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var signal = value(data("database").required("signal"), DatabaseRequestSignal.class);
            var update = value(data("database").required("update"), DatabaseRequestUpdate.class);

            traceDao.saveDatabaseRequests(List.of(new Pair<>(signal, update)));

            assertSingleRow("e_dtb_rqt", mergedRow("database", "updateRow"));
        }
    }

    @Nested
    class UpdateDatabaseRequests {
        @Test
        @Sql(SEED)
        void withMinimumRequiredFields() throws IOException {
            var update = new DatabaseRequestUpdate(TRACE_ID);

            traceDao.updateDatabaseRequests(List.of(update));

            assertExactRow(row("e_dtb_rqt", "id_dtb_rqt", TRACE_ID), mergedRow("database", "updateRowMin"));
        }

        @Test
        @Sql(SEED)
        void withAllFields() throws IOException {
            var update = value(data("database").required("update"), DatabaseRequestUpdate.class);

            traceDao.updateDatabaseRequests(List.of(update));

            assertExactRow(row("e_dtb_rqt", "id_dtb_rqt", TRACE_ID), mergedRow("database", "updateRow"));
        }
    }

    @Nested
    class SaveHttpRequestStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new HttpRequestStage(TRACE_ID, 1);

            traceDao.saveHttpRequestStages(List.of(stage));

            assertSingleRow("e_rst_rqt_stg", data("httpRequestStage").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("httpRequestStage").required("input"), HttpRequestStage.class);

            traceDao.saveHttpRequestStages(List.of(stage));

            assertSingleRow("e_rst_rqt_stg", data("httpRequestStage").required("row"));
        }
    }

    @Nested
    class SaveHttpSessionStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new HttpSessionStage(TRACE_ID, 2);

            traceDao.saveHttpSessionStages(List.of(stage));

            assertSingleRow("e_rst_ses_stg", data("httpSessionStage").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("httpSessionStage").required("input"), HttpSessionStage.class);

            traceDao.saveHttpSessionStages(List.of(stage));

            assertSingleRow("e_rst_ses_stg", data("httpSessionStage").required("row"));
        }
    }

    @Nested
    class SaveFtpRequestStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new FtpRequestStage(TRACE_ID, 1);

            traceDao.saveFtpRequestStages(List.of(stage));

            assertSingleRow("e_ftp_stg", data("ftpStage").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("ftpStage").required("input"), FtpRequestStage.class);

            traceDao.saveFtpRequestStages(List.of(stage));

            assertSingleRow("e_ftp_stg", data("ftpStage").required("row"));
        }
    }

    @Nested
    class SaveLdapRequestStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new DirectoryRequestStage(TRACE_ID, 1);

            traceDao.saveLdapRequestStages(List.of(stage));

            assertSingleRow("e_ldap_stg", data("ldapStage").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("ldapStage").required("input"), DirectoryRequestStage.class);

            traceDao.saveLdapRequestStages(List.of(stage));

            assertSingleRow("e_ldap_stg", data("ldapStage").required("row"));
        }
    }

    @Nested
    class SaveDatabaseRequestStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new DatabaseRequestStage(TRACE_ID, 1);

            traceDao.saveDatabaseRequestStages(List.of(stage));

            assertSingleRow("e_dtb_stg", data("databaseStage").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("databaseStage").required("input"), DatabaseRequestStage.class);

            traceDao.saveDatabaseRequestStages(List.of(stage));

            assertSingleRow("e_dtb_stg", data("databaseStage").required("row"));
        }
    }

    @Nested
    class SaveMailRequestStages {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var stage = new MailRequestStage(TRACE_ID, 1);

            traceDao.saveMailRequestStages(List.of(stage));

            assertSingleRow("e_smtp_stg", data("mailStage").required("rowMin"));
            assertEquals(0, count("e_smtp_mail"));
        }

        @Test
        void withAllFields() throws IOException {
            var stage = value(data("mailStage").required("input"), MailRequestStage.class);

            traceDao.saveMailRequestStages(List.of(stage));

            assertSingleRow("e_smtp_stg", data("mailStage").required("row"));
            assertSingleRow("e_smtp_mail", data("mailStage").required("mailRow"));
        }
    }

    @Nested
    class SaveExceptionTraces {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var exception = new ExceptionTrace("java.lang.IllegalStateException", "Request failed",
                    new StackTraceRow[] { new StackTraceRow("example.Service", "run", 42) }, null);
            exception.setTraceId(TRACE_ID);
            exception.setTraceType((byte) 120);

            traceDao.saveExceptionTraces(List.of(exception));

            assertSingleRow("e_exc_inf", data("exception").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var exception = value(data("exception").required("input"), ExceptionTrace.class);

            traceDao.saveExceptionTraces(List.of(exception));

            assertSingleRow("e_exc_inf", data("exception").required("row"));
        }
    }

    @Nested
    class SaveSessionEvents {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var event = new SessionEvent(START, "click", "submit", "#order-form", ">anywhere<", SESSION_ID);

            traceDao.saveSessionEvents(List.of(event));

            assertSingleRow("e_ses_evt", data("event").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var event = value(data("event").required("input"), SessionEvent.class);

            traceDao.saveSessionEvents(List.of(event));

            assertSingleRow("e_ses_evt", data("event").required("row"));
        }
    }

    @Nested
    class SaveBrowserConfigs {
        @Test
        void withMinimumRequiredFields() throws IOException {
            var config = new BrowserConfigDto();

            traceDao.saveBrowserConfigs(List.of(config));

            assertSingleRow("o_brw_cfg", data("browser").required("rowMin"));
        }

        @Test
        void withAllFields() throws IOException {
            var config = value(data("browser").required("input"), BrowserConfigDto.class);

            traceDao.saveBrowserConfigs(List.of(config));

            assertSingleRow("o_brw_cfg", data("browser").required("row"));
        }
    }

    private JsonNode data(String name) {
        return fixtures.required(name);
    }

    private <T> T value(JsonNode input, Class<T> type) {
        return defaultMapper.convertValue(input, type);
    }

    private ObjectNode mergedRow(String name, String updateRow) {
        ObjectNode expected = (ObjectNode) data(name).required("signalRow").deepCopy();
        return expected.setAll((ObjectNode) data(name).required(updateRow));
    }

    private Map<String, Object> row(String table, String key, UUID id) {
        return jdbcTemplate.queryForMap("SELECT * FROM " + table + " WHERE " + key + " = ?", id);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertSingleRow(String table, JsonNode expected) throws IOException {
        assertEquals(1, count(table));
        assertExactRow(jdbcTemplate.queryForMap("SELECT * FROM " + table), expected);
    }

    private void assertExactRow(Map<String, Object> actual, JsonNode expected) throws IOException {
        assertEquals(expected.size(), actual.size(), "Unexpected columns in persisted row");
        for (var field : expected.properties()) {
            var column = field.getKey();
            var expectedValue = field.getValue();
            assertTrue(actual.containsKey(column), "Missing column: " + column);
            var stored = actual.get(column);
            if (expectedValue.isNull()) {
                assertNull(stored, column);
            } else if (stored instanceof Timestamp timestamp) {
                assertEquals(Instant.parse(expectedValue.asString()), timestamp.toInstant(), column);
            } else if (stored instanceof UUID uuid) {
                assertEquals(UUID.fromString(expectedValue.asString()), uuid, column);
            } else if (stored instanceof Number number) {
                assertEquals(0, expectedValue.decimalValue().compareTo(new BigDecimal(number.toString())), column);
            } else if (expectedValue.isContainer()) {
                assertEquals(expectedValue, defaultMapper.readTree((String) stored), column);
            } else if (expectedValue.isBoolean()) {
                assertEquals(expectedValue.booleanValue(), stored, column);
            } else {
                assertEquals(expectedValue.asString(), stored, column);
            }
        }
    }
}
