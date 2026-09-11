package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dao.TraceDao;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TracePacket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.server.model.TraceBatchResolver.resolve;

@Slf4j
@Service
@RequiredArgsConstructor
public class TracePersistenceService implements TraceExporter {
	
	private final TraceDao dao;
	private final ExecutorService executor = wrap(newFixedThreadPool(5));

    @Override
	public void dispatch(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
	}

	@TraceableStage
	@Override
	public List<EventTrace> dispatch(boolean complete, List<EventTrace> traces) {
		return traces.isEmpty() ? emptyList() : addTraces(traces);
	}

	public List<EventTrace> addTraces(List<EventTrace> traces) {
        var cf = new ArrayList<CompletableFuture<Collection<EventTrace>>>();
        cf.add(supplyAsync(()-> {
            var unsaved = resolve(traces, MainSessionSignal.class, MainSessionUpdate.class, dao::saveMainSessionSignals, dao::updateMainSessions, dao::saveMainSessions);
            if(unsaved.isEmpty()){
                unsaved.addAll(filterAndApply(traces, (e, consumer) -> {
                    if(e instanceof SessionMaskUpdate smu && smu.isMain()) {
                        consumer.accept(smu);
                    }
                }, dao::updateMaskMainSessions));
            }
            return unsaved;
        }, executor));
        cf.add(supplyAsync(()-> {
            var unsaved = resolve(traces, HttpSessionSignal.class, HttpSessionUpdate.class, dao::saveRestSessionSignals, dao::updateRestSessions, dao::saveRestSessions);
            if(unsaved.isEmpty()){
                unsaved.addAll(filterAndApply(traces, (e, consumer) -> {
                    if(e instanceof SessionMaskUpdate smu && !smu.isMain()) {
                        consumer.accept(smu);
                    }
                }, dao::updateMaskRestSessions));
            }
            return unsaved;
        }, executor));
        
        //dual event traces
        cf.add(supplyAsync(()-> resolve(traces, HttpRequestSignal.class, HttpRequestUpdate.class, dao::saveRestRequestSignals, dao::updateRestRequests, dao::saveRestRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, LocalRequestSignal.class, LocalRequestUpdate.class, dao::saveLocalRequestSignals, dao::updateLocalRequests, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, MailRequestSignal.class, MailRequestUpdate.class, dao::saveMailRequestSignals, dao::updateMailRequests, dao::saveMailRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, FtpRequestSignal.class, FtpRequestUpdate.class, dao::saveFtpRequestSignals, dao::updateFtpRequests, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, DirectoryRequestSignal.class, DirectoryRequestUpdate.class, dao::saveLdapRequestSignals, dao::updateLdapRequests, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(()-> resolve(traces, DatabaseRequestSignal.class, DatabaseRequestUpdate.class, dao::saveDatabaseRequestSignals, dao::updateDatabaseRequests, dao::saveDatabaseRequests), executor));
        //stages
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpRequestStage.class, dao::saveHttpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, HttpSessionStage.class, dao::saveHttpSessionStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, MailRequestStage.class, dao::saveMailRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, FtpRequestStage.class, dao::saveFtpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DirectoryRequestStage.class, dao::saveLdapRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, DatabaseRequestStage.class, dao::saveDatabaseRequestStages), executor));
        //updates
        cf.add(supplyAsync(()-> filterAndApply(traces, InstanceEnvironmentUpdate.class, dao::updateInstanceEnvironments), executor));
        //events
        cf.add(supplyAsync(()-> filterAndApply(traces, SessionEvent.class, dao::saveSessionEvents), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, ExceptionTrace.class, dao::saveExceptionTraces), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, LogEntry.class, dao::saveLogEntries), executor));
        //monitoring
        cf.add(supplyAsync(()-> filterAndApply(traces, MachineResourceUsage.class, dao::saveMachineResourceUsages), executor));
        cf.add(supplyAsync(()-> filterAndApply(traces, TracePacket.class, dao::saveTracePackets), executor));

        return allOf(cf.toArray(CompletableFuture[]::new)).thenApply(v-> cf.stream()
        		.map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList()).join();
    }

    public static <U> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<U> clazz, Consumer<List<U>> saveFn) {
        return filterAndApply(c, (e, consumer) -> {
            if(clazz.isInstance(e)) {
                consumer.accept(clazz.cast(e));
            }
        }, saveFn);
    }

    public static <U> List<EventTrace> filterAndApply(Collection<EventTrace> c, BiConsumer<EventTrace, ? super Consumer<U>> mapper, Consumer<List<U>> saveFn) {
        var list = c.stream()
                .mapMulti(mapper)
                .toList();
        if(!list.isEmpty()) {
            log.debug("saving {} {}..", list.size(), list.getFirst().getClass().getSimpleName());
            try {
                saveFn.accept(list);
                list = emptyList();
            } catch (Exception e) {
                log.error("error while saving {} {}, because {}: {}", list.size(), list.getFirst().getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return (List<EventTrace>) list;
    }
}