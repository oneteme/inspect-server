package org.usf.inspect.server.service;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.server.model.TraceCorrelator.filterAndConsume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Service;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.DatabaseRequestUpdate;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.FtpRequestSignal;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.FtpRequestUpdate;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.HttpSessionSignal;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.HttpSessionUpdate;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.LocalRequestSignal;
import org.usf.inspect.core.LocalRequestUpdate;
import org.usf.inspect.core.MachineResourceUsage;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.MainSessionSignal;
import org.usf.inspect.core.MainSessionUpdate;
import org.usf.inspect.core.ProcessingQueue;
import org.usf.inspect.core.ReportEvent;
import org.usf.inspect.core.SessionEvent;
import org.usf.inspect.core.SessionMaskUpdate;
import org.usf.inspect.core.TracePublisher;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.dao.TraceDao;
import org.usf.inspect.server.dto.BrowserConfigDto;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TraceBatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TracePersister implements TracePublisher {
	
	private final TraceDao dao;
	private final ExecutorService executor = wrap(newFixedThreadPool(5));

    @Override
	public void register(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance); //sync 
	}

	@Override
	@TraceableStage
	public void flush(boolean complete, ProcessingQueue<EventTrace> queue) {
		queue.pollAll(snp->{
			mergeTraces(snp);
			return addTraces(unmodifiableList(snp));
		});
	}

	public List<EventTrace> addTraces(List<EventTrace> traces) {
        var cf = new ArrayList<CompletableFuture<Collection<EventTrace>>>();
        //sessions
        cf.add(supplyAsync(()-> filterAndConsume(traces, MainSessionSignal.class, MainSessionUpdate.class, dao::saveMainSessionSignals, dao::updateMainSessions, dao::saveMainSessions), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, HttpSessionSignal.class, HttpSessionUpdate.class, dao::saveRestSessionSignals, dao::updateRestSessions, dao::saveRestSessions), executor));
        //requests
        cf.add(supplyAsync(()-> filterAndConsume(traces, HttpRequestSignal.class, HttpRequestUpdate.class, dao::saveRestRequestSignals, dao::updateRestRequests, dao::saveRestRequests), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, LocalRequestSignal.class, LocalRequestUpdate.class, dao::saveLocalRequestSignals, dao::updateLocalRequests, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, MailRequestSignal.class, MailRequestUpdate.class, dao::saveMailRequestSignals, dao::updateMailRequests, dao::saveMailRequests), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, FtpRequestSignal.class, FtpRequestUpdate.class, dao::saveFtpRequestSignals, dao::updateFtpRequests, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, DirectoryRequestSignal.class, DirectoryRequestUpdate.class, dao::saveLdapRequestSignals, dao::updateLdapRequests, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, DatabaseRequestSignal.class, DatabaseRequestUpdate.class, dao::saveDatabaseRequestSignals, dao::updateDatabaseRequests, dao::saveDatabaseRequests), executor));
        //stages
        cf.add(supplyAsync(()-> filterAndConsume(traces, HttpRequestStage.class, dao::saveHttpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, HttpSessionStage.class, dao::saveHttpSessionStages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, MailRequestStage.class, dao::saveMailRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, FtpRequestStage.class, dao::saveFtpRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, DirectoryRequestStage.class, dao::saveLdapRequestStages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, DatabaseRequestStage.class, dao::saveDatabaseRequestStages), executor));
        //events
        cf.add(supplyAsync(()-> filterAndConsume(traces, SessionEvent.class, dao::saveSessionEvents), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, ExceptionTrace.class, dao::saveExceptionTraces), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, ReportEvent.class, dao::saveLogEntries), executor));
        //monitoring
        cf.add(supplyAsync(()-> filterAndConsume(traces, MachineResourceUsage.class, dao::saveMachineResourceUsages), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, TraceBatch.class, dao::saveTracePackets), executor));
        //browser
        cf.add(supplyAsync(() -> filterAndConsume(traces, BrowserConfigDto.class, dao::saveBrowserConfigs), executor));
        //updates
        cf.add(supplyAsync(()-> filterAndConsume(traces, InstanceEnvironmentUpdate.class, dao::updateInstanceEnvironments), executor));        
        cf.add(supplyAsync(()-> filterAndConsume(traces, (e, cons) -> {
            if(e instanceof SessionMaskUpdate upd && upd.isMain()) {
                cons.accept(upd);
            }
        }, dao::updateMaskMainSessions), executor));
        cf.add(supplyAsync(()-> filterAndConsume(traces, (e, cons) -> {
            if(e instanceof SessionMaskUpdate upd && !upd.isMain()) {
                cons.accept(upd);
            }
        }, dao::updateMaskRestSessions), executor));
        //TODO add SessionAsyncDurationUpdate
        return allOf(cf.toArray(CompletableFuture[]::new)).thenApply(v-> cf.stream()
        		.map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .toList()).join();
    }
}