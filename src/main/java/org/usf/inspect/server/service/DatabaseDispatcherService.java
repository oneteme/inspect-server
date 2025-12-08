package org.usf.inspect.server.service;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.groupingBy;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.server.service.TraceBatchResolver.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

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
        cf.add(supplyAsync(()-> {
            var unsaved = resolve(traces, MainSession2.class, MainSessionCallback.class, dao::savePartialMainSessions, dao::updateMainSessions, dao::saveCompleteMainSessions);
            if(unsaved.isEmpty()){
                unsaved.addAll(filterAndApply(traces, (e, consumer) -> {
                    if(e instanceof SessionMaskUpdate smu && smu.isMain()) {
                        consumer.accept(smu);
                    }
                }, dao::updateMaskMainSessions, SessionMaskUpdate.class));
            }
            return unsaved;
        }, executor));
        cf.add(supplyAsync(()-> {
            var unsaved = resolve(traces, HttpSession2.class, HttpSessionCallback.class, dao::savePartialRestSessions, dao::updateRestSessions, dao::saveCompleteRestSessions);
            if(unsaved.isEmpty()){
                unsaved.addAll(filterAndApply(traces, (e, consumer) -> {
                    if(e instanceof SessionMaskUpdate smu && !smu.isMain()) {
                        consumer.accept(smu);
                    }
                }, dao::updateMaskRestSessions, SessionMaskUpdate.class));
            }
            return unsaved;
        }, executor));
        cf.add(supplyAsync(()-> resolve(traces, HttpRequest2.class, HttpRequestCallback.class, dao::savePartialRestRequests, dao::updateRestRequests, dao::saveCompleteRestRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, LocalRequest2.class, LocalRequestCallback.class, dao::savePartialLocalRequests, dao::updateLocalRequests, dao::saveCompleteLocalRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, MailRequest2.class, MailRequestCallback.class, dao::savePartialMailRequests, dao::updateMailRequests, dao::saveCompleteMailRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, FtpRequest2.class, FtpRequestCallback.class, dao::savePartialFtpRequests, dao::updateFtpRequests, dao::saveCompleteFtpRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, DirectoryRequest2.class, DirectoryRequestCallback.class, dao::savePartialLdapRequests, dao::updateLdapRequests, dao::saveCompleteLdapRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, DatabaseRequest2.class, DatabaseRequestCallback.class, dao::savePartialDatabaseRequests, dao::updateDatabaseRequests, dao::saveCompleteDatabaseRequests), executor));
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

    public static <U extends EventTrace> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<U> clazz, Consumer<List<U>> saveFn) {
        return filterAndApply(c, (e, consumer) -> {
            if(clazz.isInstance(e)) {
                consumer.accept(clazz.cast(e));
            }
        }, saveFn, clazz);
    }

    public static <U extends EventTrace> List<EventTrace> filterAndApply(Collection<EventTrace> c, BiConsumer<EventTrace, ? super Consumer<U>> mapper, Consumer<List<U>> saveFn, Class<U> clazz) {
        var list = c.stream()
                .mapMulti(mapper)
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
}


