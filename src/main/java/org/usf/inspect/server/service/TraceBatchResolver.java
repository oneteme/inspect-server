package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class TraceBatchResolver<T extends Initializer, U extends Callback>  {

    private final Class<T> initClazz;
    private final Class<U> callbackClazz;

    private final Consumer<List<T>> insertPartialBatchExecutor;
    private final Consumer<List<U>> updateBatchExecutor;
    private final Consumer<List<Pair<T, U>>> insertCompleteBatchExecutor;
    private final Function<List<U>, U> reduceCallback;

    public List<EventTrace> resolve(Collection<EventTrace> traces){
        var map = traces.stream().filter(o-> initClazz.isInstance(o) || callbackClazz.isInstance(o) )
                .map(Compleatable.class::cast)
                .collect(Collectors.groupingBy(Compleatable::getId));

        List<T> requests = new ArrayList<>();
        List<U> callbackRequests = new ArrayList<>();
        List<Pair<T, U>> completeRequests = new ArrayList<>();

        for(var o : map.values()){
            if(o.size() == 1) {
                if(initClazz.isInstance(o.getFirst())) {
                    requests.add(initClazz.cast(o.getFirst()));
                } else if(callbackClazz.isInstance(o.getFirst())) {
                    callbackRequests.add(callbackClazz.cast(o.getFirst()));
                }
            } else if(o.size() > 1) {
                var calls = o.stream().filter(callbackClazz::isInstance).map(callbackClazz::cast).toList();
                if(calls.size() == o.size()-1) {
                    var call = reduceCallback.apply(calls);
                    var init = o.stream().filter(initClazz::isInstance).map(initClazz::cast).findFirst().orElseThrow(()-> new IllegalStateException("No initializer found"));
                    completeRequests.add(new Pair<>(init, call));
                }
            }
        }
        var res = new ArrayList<EventTrace>();
        if(!requests.isEmpty()) {
            try {
                insertPartialBatchExecutor.accept(requests);
            } catch (Exception e) {
                log.error("error while resolving init requests: {}", e.getMessage());
                res.addAll(requests);
            }
        }
        if(!callbackRequests.isEmpty()) {
            try {
                updateBatchExecutor.accept(callbackRequests);
            } catch (Exception e) {
                log.error("error while resolving callback requests: {}", e.getMessage());
                res.addAll(callbackRequests);
            }
        }
        if(!completeRequests.isEmpty()) {
            try {
                insertCompleteBatchExecutor.accept(completeRequests);
            } catch (Exception e) {
                log.error("error while resolving complete requests: {}", e.getMessage());
                res.addAll(completeRequests.stream().flatMap(p-> Stream.of(p.getV1(), p.getV2())).toList());
            }
        }
        return res;
    }

    public static <T extends Initializer, U extends Callback, R> List<EventTrace> resolve(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, Consumer<List<T>> insertPartialBatchExecutor, Consumer<List<U>> updateBatchExecutor, Consumer<List<Pair<T, U>>> insertCompleteBatchExecutor, Function<List<U>, U> reduceCallback) {
        return new TraceBatchResolver<>(initClazz, callClazz, insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor, reduceCallback).resolve(c);
    }

    public static <T extends Initializer, U extends Callback, R> List<EventTrace> resolve(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, Consumer<List<T>> insertPartialBatchExecutor, Consumer<List<U>> updateBatchExecutor, Consumer<List<Pair<T, U>>> insertCompleteBatchExecutor) {
        return new TraceBatchResolver<>(initClazz, callClazz, insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor, TraceBatchResolver::defaultReduceCallback).resolve(c);
    }

    public static <T extends Initializer, U extends Callback> void resolve(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, Function<List<U>, U> reduceCallback, InstanceTrace instanceTrace) {
        resolve(c, initClazz, callClazz, sessions -> {
            instanceTrace.addPending(sessions.size());
            instanceTrace.addTraceCount(sessions.size());
        }, sessions -> {}, sessions -> {
            instanceTrace.addTraceCount(sessions.size());
        }, reduceCallback);
    }

    public static <U> U defaultReduceCallback(List<U> traces) {
        if(traces.size() == 1) {
            return traces.getFirst();
        }
        throw new IllegalStateException("Multiple callbacks found for the same identifier, unable to reduce the list.");
    }

    public static <U extends AbstractSessionCallback> U reduceCallback(List<U> traces) {
        return traces.stream().max(Comparator.comparingLong(a -> a.getRequestMask().get())).orElseThrow(() -> new IllegalStateException("No callback found"));
    }
}



