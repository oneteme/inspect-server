package org.usf.inspect.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.usf.inspect.core.TraceUpdate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.TraceSignal;
import org.usf.inspect.server.model.Pair;
import org.usf.inspect.server.model.TraceBatchResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceBatchResolverTest {

    @Mock
    private Consumer<List<TestInitializer>> insertPartialBatchExecutor;

    @Mock
    private Consumer<List<TestCallback>> updateBatchExecutor;

    @Mock
    private Consumer<List<Pair<TestInitializer, TestCallback>>> insertCompleteBatchExecutor;

    @Captor
    private ArgumentCaptor<List<TestInitializer>> initializerCaptor;

    @Captor
    private ArgumentCaptor<List<TestCallback>> callbackCaptor;

    @Captor
    private ArgumentCaptor<List<Pair<TestInitializer, TestCallback>>> completeCaptor;

    private TraceBatchResolver<TestInitializer, TestCallback> resolver;

    @BeforeEach
    void setUp() {
        resolver = new TraceBatchResolver<>(
                TestInitializer.class,
                TestCallback.class,
                insertPartialBatchExecutor,
                updateBatchExecutor,
                insertCompleteBatchExecutor
        );
    }

    @Test
    void testResolve_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        List<EventTrace> traces = emptyList();

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor);
    }

    @Test
    void testResolve_WithOnlyInitializers_ShouldCallInsertPartialBatch() {
        // Given
        List<EventTrace> traces = List.of(
                new TestInitializer("id1", Instant.now()),
                new TestInitializer("id2", Instant.now())
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verify(insertPartialBatchExecutor, times(1)).accept(initializerCaptor.capture());
        assertEquals(2, initializerCaptor.getValue().size());
        verifyNoInteractions(updateBatchExecutor, insertCompleteBatchExecutor);
    }

    @Test
    void testResolve_WithOnlyCallbacks_ShouldCallUpdateBatch() {
        // Given
        List<EventTrace> traces = List.of(
                new TestCallback("id1", Instant.now()),
                new TestCallback("id2", Instant.now())
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verify(updateBatchExecutor, times(1)).accept(callbackCaptor.capture());
        assertEquals(2, callbackCaptor.getValue().size());
        verifyNoInteractions(insertPartialBatchExecutor, insertCompleteBatchExecutor);
    }

    @Test
    void testResolve_WithMatchingInitializerAndCallback_ShouldCallInsertCompleteBatch() {
        // Given
        String id = "id1";
        Instant start = Instant.now();
        Instant end = start.plusSeconds(10);

        List<EventTrace> traces = List.of(
                new TestInitializer(id, start),
                new TestCallback(id, end)
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verify(insertCompleteBatchExecutor, times(1)).accept(completeCaptor.capture());
        assertEquals(1, completeCaptor.getValue().size());

        Pair<TestInitializer, TestCallback> pair = completeCaptor.getValue().getFirst();
        assertEquals(id, pair.getV1().getId());
        assertEquals(id, pair.getV2().getId());
        assertEquals(start, pair.getV1().getStart());
        assertEquals(end, pair.getV2().getEnd());

        verifyNoInteractions(insertPartialBatchExecutor, updateBatchExecutor);
    }

    @Test
    void testResolve_WithMixedTraces_ShouldCallAllAppropriateExecutors() {
        // Given
        Instant now = Instant.now();
        List<EventTrace> traces = List.of(
                new TestInitializer("id1", now),                    // Sans callback -> partial
                new TestInitializer("id2", now),                    // Avec callback -> complete
                new TestCallback("id2", now.plusSeconds(5)),
                new TestCallback("id3", now.plusSeconds(10))        // Sans initializer -> callback seul
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());

        verify(insertPartialBatchExecutor, times(1)).accept(initializerCaptor.capture());
        assertEquals(1, initializerCaptor.getValue().size());
        assertEquals("id1", initializerCaptor.getValue().getFirst().getId());

        verify(updateBatchExecutor, times(1)).accept(callbackCaptor.capture());
        assertEquals(1, callbackCaptor.getValue().size());
        assertEquals("id3", callbackCaptor.getValue().getFirst().getId());

        verify(insertCompleteBatchExecutor, times(1)).accept(completeCaptor.capture());
        assertEquals(1, completeCaptor.getValue().size());
        assertEquals("id2", completeCaptor.getValue().getFirst().getV1().getId());
    }

    @Test
    void testResolve_WithMultipleInitializersSameId_ShouldSelectEarliest() {
        // Given
        String id = "id1";
        Instant early = Instant.now();
        Instant late = early.plusSeconds(10);

        List<EventTrace> traces = List.of(
                new TestInitializer(id, late),
                new TestInitializer(id, early)
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());

        verify(insertPartialBatchExecutor, times(1)).accept(initializerCaptor.capture());
        assertEquals(1, initializerCaptor.getValue().size());
        assertEquals(early, initializerCaptor.getValue().getFirst().getStart());
    }

    @Test
    void testResolve_WithMultipleCallbacksSameId_ShouldSelectLatest() {
        // Given
        String id = "id1";
        Instant early = Instant.now();
        Instant late = early.plusSeconds(10);

        List<EventTrace> traces = List.of(
                new TestCallback(id, early),
                new TestCallback(id, late)
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verify(updateBatchExecutor, times(1)).accept(callbackCaptor.capture());
        assertEquals(1, callbackCaptor.getValue().size());
        assertEquals(late, callbackCaptor.getValue().getFirst().getEnd());
    }

    @Test
    void testResolve_WhenInsertPartialThrowsException_ShouldReturnFailedTraces() {
        // Given
        List<EventTrace> traces = List.of(
                new TestInitializer("id1", Instant.now())
        );
        doThrow(new RuntimeException("Database error")).when(insertPartialBatchExecutor).accept(anyList());

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertEquals(1, result.size());
        assertInstanceOf(TestInitializer.class, result.getFirst());
    }

    @Test
    void testResolve_WhenUpdateBatchThrowsException_ShouldReturnFailedTraces() {
        // Given
        List<EventTrace> traces = List.of(
                new TestCallback("id1", Instant.now())
        );
        doThrow(new RuntimeException("Database error")).when(updateBatchExecutor).accept(anyList());

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertEquals(1, result.size());
        assertInstanceOf(TestCallback.class, result.getFirst());
    }

    @Test
    void testResolve_WhenInsertCompleteThrowsException_ShouldReturnBothInitAndCallback() {
        // Given
        String id = "id1";
        List<EventTrace> traces = List.of(
                new TestInitializer(id, Instant.now()),
                new TestCallback(id, Instant.now().plusSeconds(5))
        );
        doThrow(new RuntimeException("Database error")).when(insertCompleteBatchExecutor).accept(anyList());

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t instanceof TestInitializer));
        assertTrue(result.stream().anyMatch(t -> t instanceof TestCallback));
    }

    @Test
    void testResolve_WithNonCompletableTraces_ShouldIgnoreThem() {
        // Given
        List<EventTrace> traces = new ArrayList<>();
        traces.add(new TestInitializer("id1", Instant.now()));
        traces.add(new NonCompletableTrace()); // Trace qui n'est pas Initializer ni Callback

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());
        verify(insertPartialBatchExecutor, times(1)).accept(initializerCaptor.capture());
        assertEquals(1, initializerCaptor.getValue().size());
    }

    @Test
    void testResolve_WithMultipleCompleteAndPartialTraces_ShouldProcessCorrectly() {
        // Given
        Instant now = Instant.now();
        List<EventTrace> traces = List.of(
                new TestInitializer("id1", now),
                new TestCallback("id1", now.plusSeconds(1)),
                new TestInitializer("id2", now.plusSeconds(2)),
                new TestCallback("id2", now.plusSeconds(3)),
                new TestInitializer("id3", now.plusSeconds(4)),
                new TestCallback("id4", now.plusSeconds(5))
        );

        // When
        List<EventTrace> result = resolver.resolve(traces);

        // Then
        assertTrue(result.isEmpty());

        // 2 paires complètes
        verify(insertCompleteBatchExecutor, times(1)).accept(completeCaptor.capture());
        assertEquals(2, completeCaptor.getValue().size());

        // 1 initializer seul
        verify(insertPartialBatchExecutor, times(1)).accept(initializerCaptor.capture());
        assertEquals(1, initializerCaptor.getValue().size());

        // 1 callback seul
        verify(updateBatchExecutor, times(1)).accept(callbackCaptor.capture());
        assertEquals(1, callbackCaptor.getValue().size());
    }

    // Classes de test internes

    static class TestInitializer implements TraceSignal {
        private final String id;
        private final Instant start;

        TestInitializer(String id, Instant start) {
            this.id = id;
            this.start = start;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Instant getStart() {
            return start;
        }
    }

    static class TestCallback implements TraceUpdate {
        private final String id;
        private final Instant end;

        TestCallback(String id, Instant end) {
            this.id = id;
            this.end = end;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Instant getEnd() {
            return end;
        }
        
        @Override
        public void setEnd(Instant end) {
        	// TODO Auto-generated method stub
        	
        }
    }

    static class NonCompletableTrace implements EventTrace {
        // Trace qui n'implémente pas CompletableTrace
    }
}

