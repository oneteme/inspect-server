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
import java.util.stream.Collectors;

import static org.usf.inspect.core.SessionContextManager.*;
import static org.usf.inspect.core.SessionContextManager.emitWarn;

@Slf4j
@RequiredArgsConstructor
public class TraceBatchResolver<T extends TraceSignal, U extends TraceUpdate>  {

    private final Class<T> initClazz;
    private final Class<U> callbackClazz;

    private final Consumer<List<T>> insertPartialBatchExecutor;
    private final Consumer<List<U>> updateBatchExecutor;
    private final Consumer<List<Pair<T, U>>> insertCompleteBatchExecutor;

    public List<EventTrace> resolve(Collection<EventTrace> traces){
        var map = traces.stream().filter(o-> initClazz.isInstance(o) || callbackClazz.isInstance(o))
                .map(TracePart.class::cast)
                .collect(Collectors.groupingBy(TracePart::getId));

        List<T> initializers = new ArrayList<>();
        List<U> callbacks = new ArrayList<>();
        List<Pair<T, U>> completes = new ArrayList<>();

        for(var o : map.values()){
            var inits = o.stream().filter(initClazz::isInstance).map(initClazz::cast).toList();
            var calls = o.stream().filter(callbackClazz::isInstance).map(callbackClazz::cast).toList();
            if(inits.size() > 1) {
                log.warn("Multiple {} found for the same identifier, unable to reduce the list.", initClazz.getSimpleName());
                emitWarn("Multiple " + initClazz.getSimpleName() + " found for the same identifier, unable to reduce the list.");
            }
            if(calls.size() > 1) {
                log.warn("Multiple {} found for the same identifier, unable to reduce the list.", callbackClazz.getSimpleName());
                emitWarn("Multiple " + callbackClazz.getSimpleName() + " found for the same identifier, unable to reduce the list.");
            }
            var init = inits.stream().min(Comparator.comparing(TraceSignal::getStart));
            var call = calls.stream().max(Comparator.comparing(TraceUpdate::getEnd));
            if(init.isPresent() && call.isPresent()) {
                completes.add(new Pair<>(init.get(), call.get()));
            } else if(call.isPresent()) {
                callbacks.add(callbackClazz.cast(call.get()));
            } else {
                initializers.add(initClazz.cast(init.get()));
            }
        }
        var res = new ArrayList<EventTrace>();
        if(!initializers.isEmpty()) {
            try {
                insertPartialBatchExecutor.accept(initializers);
            } catch (Exception e) {
                log.error("error while resolving init requests: {}", e.getMessage());
                res.addAll(initializers);
            }
        }
        if(!callbacks.isEmpty()) {
            try {
                updateBatchExecutor.accept(callbacks);
            } catch (Exception e) {
                log.error("error while resolving callback requests: {}", e.getMessage());
                res.addAll(callbacks);
            }
        }
        if(!completes.isEmpty()) {
            try {
                insertCompleteBatchExecutor.accept(completes);
            } catch (Exception e) {
                log.error("error while resolving complete requests: {}", e.getMessage());
                completes.forEach(ent->{
                    res.add(ent.getV1());
                    res.add(ent.getV2());
                });
            }
        }
        return res;
    }

    public static <T extends TraceSignal, U extends TraceUpdate> List<EventTrace> resolve(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, Consumer<List<T>> insertPartialBatchExecutor, Consumer<List<U>> updateBatchExecutor, Consumer<List<Pair<T, U>>> insertCompleteBatchExecutor) {
        return new TraceBatchResolver<>(initClazz, callClazz, insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor).resolve(c);
    }


    public static void resolve(Collection<EventTrace> c, InstanceTrace instanceTrace) {
        resolve(c, TraceSignal.class, TraceUpdate.class,
            sessions -> {
                instanceTrace.addPending(sessions.size());
                instanceTrace.addTraceCount(sessions.size());
            },
            sessions -> instanceTrace.removePending(sessions.size()),
            sessions -> instanceTrace.addTraceCount(sessions.size())
        );
    }
}



