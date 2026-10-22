package org.usf.inspect.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.usf.inspect.core.DispatchState.COLLECT;
import static org.usf.inspect.core.DispatchState.DISABLE;
import static org.usf.inspect.server.JsonUtils.defaultMapper;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.INSPECT_ID;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.serverHub;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.usf.inspect.EventTrace;
import org.usf.inspect.HttpRequestSignal;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.InstanceType;
import org.usf.inspect.MainSessionSignal;
import org.usf.inspect.ReportEvent;
import org.usf.inspect.SystemMetrics;
import org.usf.inspect.core.ProcessingQueue;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;
import org.usf.inspect.test.InspectTest;

@InspectTest
@SpringBootTest
@ActiveProfiles("test")
class TraceServiceH2Test {

    private static final Instant START = Instant.parse("2026-01-15T10:00:00Z");
    private static final UUID PERSISTED_INSTANCE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private TraceService traceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanState() {
        traceService.updateState(COLLECT);
        clearQueue();
        executeSql("cleanup.sql");
    }

    @Test
    void addInstance_ShouldPersistInstanceAndQueueEmptyBatch_WhenInstanceIsValid() throws Exception {
        var id = UUID.randomUUID();
        var instance = new InstanceEnvironment(id, START, InstanceType.SERVER,
                "inspect-test", "1.5.0", "test", "192.0.2.10", "Windows", "Java 21",
                "service", "main", "abc123", "spring-collector", Map.of(), null);
        instance.setNamespace("NOVA_DEV");

        traceService.addInstance(instance, 3);

        assertEquals("NOVA_DEV", instance.getNamespace());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM e_env_ins WHERE id_ins = ? AND cd_nsp = ?",
                Integer.class, id, "NOVA_DEV"));

        var traces = List.copyOf(traceService.peekQueue());
        assertEquals(1, traces.size());
        var batch = assertInstanceOf(TraceBatch.class, traces.get(0));
        assertEquals(id, batch.getInstanceId());
        assertEquals(3, batch.getAttempts());
        assertEquals(0, batch.getSequence());
        assertEquals(0, batch.getTraceCount());
        assertEquals(0, batch.getPending());
    }

    @Test
    void addTraces_ShouldAssignInstanceAndPreserveQueueOrder_WhenTracesAndEndAreProvided() throws Exception {
        var instanceId = UUID.randomUUID();
        var end = START.plusSeconds(30);
        var request = new HttpRequestSignal(UUID.randomUUID(), UUID.randomUUID(), START, "http-worker");
        var session = new MainSessionSignal(UUID.randomUUID(), START, "main-worker", "SCHEDULE");
        var metrics = new SystemMetrics(START, 128, 512, 1024, 8, 24, (short) 1250);
        var report = new ReportEvent(START, "main", "test", "message", null, null);
        var input = List.<EventTrace>of(request, session, metrics, report);

        traceService.addTraces(instanceId, 7, 2, end, input);

        assertEquals(instanceId, request.getInstanceId());
        assertEquals(instanceId, session.getInstanceId());
        assertEquals(instanceId, metrics.getInstanceId());
        assertEquals(instanceId, report.getInstanceId());

        var queued = List.copyOf(traceService.peekQueue());
        assertEquals(6, queued.size());
        var update = assertInstanceOf(InstanceEnvironmentUpdate.class, queued.get(0));
        assertEquals(instanceId, update.getId());
        assertEquals(end, update.getEnd());
        assertSame(request, queued.get(1));
        assertSame(session, queued.get(2));
        assertSame(metrics, queued.get(3));
        assertSame(report, queued.get(4));

        var batch = assertInstanceOf(TraceBatch.class, queued.get(5));
        assertEquals(instanceId, batch.getInstanceId());
        assertEquals(7, batch.getSequence());
        assertEquals(2, batch.getAttempts());
        assertEquals(0, batch.getTraceCount());
        assertEquals(2, batch.getPending());
    }

    @Test
    void addTraces_ShouldQueueOnlyEmptyBatch_WhenTracesAndEndAreNull() throws Exception {
        var instanceId = UUID.randomUUID();

        traceService.addTraces(instanceId, 4, 1, null, null);

        var queued = List.copyOf(traceService.peekQueue());
        assertEquals(1, queued.size());
        var batch = assertInstanceOf(TraceBatch.class, queued.get(0));
        assertEquals(instanceId, batch.getInstanceId());
        assertEquals(4, batch.getSequence());
        assertEquals(1, batch.getAttempts());
        assertEquals(0, batch.getTraceCount());
        assertEquals(0, batch.getPending());
    }

    @Test
    void peekQueue_ShouldReturnUnmodifiableSnapshot_WhenServiceHasQueuedTraces() throws Exception {
        var request = new HttpRequestSignal(UUID.randomUUID(), UUID.randomUUID(), START, "http-worker");

        traceService.addTraces(UUID.randomUUID(), 1, 0, null, List.of(request));

        var snapshot = traceService.peekQueue();
        assertEquals(2, snapshot.size());
        assertSame(request, snapshot.iterator().next());
        assertInstanceOf(TraceBatch.class, snapshot.stream().skip(1).findFirst().orElseThrow());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void updateState_ShouldExposeNewState_WhenHubIsEnabled() {
        try {
            traceService.updateState(DISABLE);

            assertEquals(DISABLE, traceService.getDispatcherState());
        } finally {
            traceService.updateState(COLLECT);
        }
        assertEquals(COLLECT, traceService.getDispatcherState());
    }

    @Test
    void hasBeenTraced_ShouldReturnTrue_WhenBatchIsQueued() throws Exception {
        var instanceId = UUID.randomUUID();
        traceService.addTraces(instanceId, 9, 1, null, List.of());

        assertTrue(traceService.hasBeenTraced(instanceId, 9));
    }

    @Test
    void hasBeenTraced_ShouldReturnTrue_WhenBatchIsPersisted() {
        executeSql("jdd-insert-trace-service-test.sql");

        assertTrue(traceService.hasBeenTraced(PERSISTED_INSTANCE_ID, 12));
    }

    @Test
    void hasBeenTraced_ShouldReturnFalse_WhenBatchIsAbsent() {
        assertFalse(traceService.hasBeenTraced(UUID.randomUUID(), 15));
    }

    @Test
    void onApplicationEvent_ShouldRequeueOriginalTrace_WhenRetryIsRequested() {
        var request = new HttpRequestSignal(UUID.randomUUID(), UUID.randomUUID(), START, "http-worker");

        traceService.onApplicationEvent(new UnsavedEventTraceEvent(this, request, true));

        var queued = List.copyOf(traceService.peekQueue());
        assertEquals(1, queued.size());
        assertSame(request, queued.get(0));
    }

    @Test
    void onApplicationEvent_ShouldQueueReport_WhenRequestCannotBeSaved() throws Exception {
        var instanceId = UUID.randomUUID();
        var request = new HttpRequestSignal(UUID.randomUUID(), UUID.randomUUID(), START, "http-worker");
        request.setInstanceId(instanceId);
        var expectedJson = defaultMapper.writeValueAsString(request);

        traceService.onApplicationEvent(new UnsavedEventTraceEvent(this, request, false));

        var queued = List.copyOf(traceService.peekQueue());
        assertEquals(1, queued.size());
        var report = assertInstanceOf(ReportEvent.class, queued.get(0));
        assertEquals(instanceId, report.getInstanceId());
        assertEquals("saveTrace", report.getAction());
        assertEquals(expectedJson, report.getException());
        assertNotNull(report.getInstant());
    }

    @Test
    void onApplicationEvent_ShouldNotQueueReport_WhenInstanceIdIsMissing() {
        var request = new HttpRequestSignal(UUID.randomUUID(), UUID.randomUUID(), START, "http-worker");

        traceService.onApplicationEvent(new UnsavedEventTraceEvent(this, request, false));
        assertTrue(traceService.peekQueue().stream()
                .allMatch(t -> t instanceof ReportEvent report && INSPECT_ID.equals(report.getInstanceId())));
    }

    @SuppressWarnings("unchecked")
    private ProcessingQueue<EventTrace> queue() {
        var queue = (ProcessingQueue<EventTrace>) ReflectionTestUtils.getField(serverHub(), "queue");
        assertNotNull(queue);
        return queue;
    }

    private void clearQueue() {
        queue().pollAll(ignored -> List.of());
    }

    private void executeSql(String fileName) {
        var script = new ClassPathResource("data/trace-service/" + fileName);
        new ResourceDatabasePopulator(script).execute(jdbcTemplate.getDataSource());
    }
}
