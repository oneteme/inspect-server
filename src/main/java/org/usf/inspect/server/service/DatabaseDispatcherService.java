package org.usf.inspect.server.service;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
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
        cf.add(supplyAsync(()-> filterAndApply(traces, MainSession.class, dao::saveMainSessions), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, RestSession.class, dao::saveRestSessions), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, RestRequest.class, dao::saveRestRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, LocalRequest.class, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MailRequest.class, dao::saveMailRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, FtpRequest.class, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DirectoryRequest.class, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DatabaseRequest.class, dao::saveDatabaseRequests), executor));
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

    public List<EventTrace> addTraces2(List<EventTrace> traces) {
        var cf = new ArrayList<CompletableFuture<Collection<EventTrace>>>();
        cf.add(supplyAsync(()-> filterAndApply(traces, MainSession.class, dao::saveMainSessions), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, RestSession.class, dao::saveRestSessions), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, RestRequest.class, dao::saveRestRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, LocalRequest.class, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MailRequest.class, dao::saveMailRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, FtpRequest.class, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DirectoryRequest.class, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DatabaseRequest.class, dao::saveDatabaseRequests), executor));
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

    static <U extends EventTrace, A extends EventTrace> List<EventTrace> filterAndApply2(Collection<EventTrace> c, Class<U> clazz, Class<A> clazz2,  BiConsumer<List<U>, List<A>> saveFn) {
        List<EventTrace> result = new ArrayList<>();
        var list = c.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
        var list2 = c.stream()
                .filter(clazz2::isInstance)
                .map(clazz2::cast)
                .toList();
        if(!list.isEmpty() || !list2.isEmpty()) {
            log.debug("saving {} {}..", list.size(), clazz.getSimpleName());
            log.debug("saving {} {}..", list2.size(), clazz2.getSimpleName());
            try {
                saveFn.accept(list, list2);
                list = emptyList();
                list2 = emptyList();
            } catch (Exception e) {
                log.error("error while saving {} {}, because {}: {}", list.size(), clazz.getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        result.addAll(list);
        result.addAll(list2);
        return result;
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
}
