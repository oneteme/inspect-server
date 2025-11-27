package org.usf.inspect.server.service;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.groupingBy;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dao.TraceDao;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseDispatcherService implements DispatcherAgent {
	
	private final TraceDao dao;
	private final ObjectMapper mapper;
	private final ExecutorService executor = wrap(newFixedThreadPool(5));

    @Override
	public void dispatch(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
	}

	@TraceableStage
	@Override
	public List<EventTrace> dispatch(boolean complete, int attempts, int pending, List<EventTrace> traces) {
		return traces.isEmpty() ? emptyList() : addTraces(traces);
	}

	@Override
	public void dispatch(int attempts, File dumpFile) {
		try {
			var traces = mapper.readValue(dumpFile, new TypeReference<List<EventTrace>>() {});
			dispatch(false, attempts, 0, traces);
		} catch (IOException e) {
			throw new DispatchException("cannot dispatch dumpFile " + dumpFile.getName(), e);
		}
	}
	
	public List<EventTrace> addTraces(List<EventTrace> traces) {
        var cf = new ArrayList<CompletableFuture<Collection<EventTrace>>>();
        cf.add(supplyAsync(()-> filterAndApply(traces, MainSession2.class, MainSessionCallback.class, dao::savePartialMainSessions, dao::updateMainSessions, dao::saveCompleteMainSessions, DatabaseDispatcherService::reduceCallback), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpSession2.class, HttpSessionCallback.class, dao::savePartialRestSessions, dao::updateRestSessions, dao::saveCompleteRestSessions, DatabaseDispatcherService::reduceCallback), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpRequest2.class, HttpRequestCallback.class, dao::savePartialRestRequests, dao::updateRestRequests, dao::saveCompleteRestRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, LocalRequest2.class, LocalRequestCallback.class, dao::savePartialLocalRequests, dao::updateLocalRequests, dao::saveCompleteLocalRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MailRequest2.class, MailRequestCallback.class, dao::savePartialMailRequests, dao::updateMailRequests, dao::saveCompleteMailRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, FtpRequest2.class, FtpRequestCallback.class, dao::savePartialFtpRequests, dao::updateFtpRequests, dao::saveCompleteFtpRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DirectoryRequest2.class, DirectoryRequestCallback.class, dao::savePartialLdapRequests, dao::updateLdapRequests, dao::saveCompleteLdapRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DatabaseRequest2.class, DatabaseRequestCallback.class, dao::savePartialDatabaseRequests, dao::updateDatabaseRequests, dao::saveCompleteDatabaseRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpRequestStage.class, dao::saveHttpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpSessionStage.class, dao::saveHttpSessionStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MailRequestStage.class, dao::saveMailRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, FtpRequestStage.class, dao::saveFtpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DirectoryRequestStage.class, dao::saveLdapRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DatabaseRequestStage.class, dao::saveDatabaseRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MachineResourceUsage.class, dao::saveMachineResourceUsages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, LogEntry.class, dao::saveLogEntries), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, InstanceEnvironmentUpdate.class, dao::updateInstanceEnvironments), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, InstanceTrace.class, dao::saveInstanceTraces), executor));

        return allOf(cf.toArray(CompletableFuture[]::new)).thenApply(v-> cf.stream()
        		.map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList()).join();
    }

    @RequiredArgsConstructor
    static class TraceBatchResolver<T extends Initializer, U extends Callback>  {

        private final Class<T> initClazz;
        private final Class<U> callbackClazz;

        private final ToLongFunction<List<T>> insertPartialBatchExecutor;
        private final ToLongFunction<List<U>> updateBatchExecutor;
        private final ToLongFunction<List<Pair<T, U>>> insertCompleteBatchExecutor;
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
                    insertPartialBatchExecutor.applyAsLong(requests);
                } catch (Exception e) {
                    log.error("error while resolving init requests: {}", e.getMessage());
                    res.addAll(requests);
                }
            }
            if(!callbackRequests.isEmpty()) {
                try {
                    updateBatchExecutor.applyAsLong(callbackRequests);
                } catch (Exception e) {
                    log.error("error while resolving callback requests: {}", e.getMessage());
                    res.addAll(callbackRequests);
                }
            }
            if(!completeRequests.isEmpty()) {
                try {
                    insertCompleteBatchExecutor.applyAsLong(completeRequests);
                } catch (Exception e) {
                    log.error("error while resolving complete requests: {}", e.getMessage());
                    res.addAll(completeRequests.stream().flatMap(p-> Stream.of(p.v1, p.v2)).toList());
                }
            }
            return res;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class Pair<R,C>{
        final R v1;
        final C v2;
    }

    static <U extends EventTrace> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<U> clazz, Consumer<List<U>> saveFn) {
        var list = c.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
        if(!list.isEmpty()) {
            log.debug("saving {} {}..", list.size(), clazz.getSimpleName());
            try {
                saveFn.accept(list);
                list = emptyList();
            } catch (Exception e) {
                log.error("error while saving {} {}, because {}: {}", list.size(), clazz.getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return (List<EventTrace>) list;
    }

    <T extends Initializer, U extends Callback> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, ToLongFunction<List<T>> insertPartialBatchExecutor, ToLongFunction<List<U>> updateBatchExecutor, ToLongFunction<List<Pair<T, U>>> insertCompleteBatchExecutor, Function<List<U>, U> reduceCallback) {
        return new TraceBatchResolver<>(initClazz, callClazz, insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor, reduceCallback).resolve(c);
    }

    <T extends Initializer, U extends Callback> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<T> initClazz, Class<U> callClazz, ToLongFunction<List<T>> insertPartialBatchExecutor, ToLongFunction<List<U>> updateBatchExecutor, ToLongFunction<List<Pair<T, U>>> insertCompleteBatchExecutor) {
        return new TraceBatchResolver<>(initClazz, callClazz, insertPartialBatchExecutor, updateBatchExecutor, insertCompleteBatchExecutor, DatabaseDispatcherService::defaultReduceCallback).resolve(c);
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


