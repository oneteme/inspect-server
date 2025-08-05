package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.*;
import org.usf.inspect.server.dao.TraceDao;
import org.usf.inspect.server.model.InstanceTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TraceService {
    private final TraceDao dao;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public void addInstance(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    public void updateInstance(Instant end, String instanceId){
        dao.updateInstanceEnvironment(end, instanceId);
    }

    public void addInstanceTrace(InstanceTrace instanceTrace){
        dao.saveInstanceTrace(instanceTrace);
    }

    public List<EventTrace> addTraces(List<EventTrace> eventTraces) {
        var cf = new ArrayList<CompletableFuture<Collection<EventTrace>>>();
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MainSession.class, dao::saveMainSessions), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, RestSession.class, dao::saveRestSessions), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, RestRequest.class, dao::saveRestRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, LocalRequest.class, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MailRequest.class, dao::saveMailRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, FtpRequest.class, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DirectoryRequest.class, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DatabaseRequest.class, dao::saveDatabaseRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, HttpRequestStage.class, dao::saveHttpRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, HttpSessionStage.class, dao::saveHttpSessionStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MailRequestStage.class, dao::saveMailRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, FtpRequestStage.class, dao::saveFtpRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DirectoryRequestStage.class, dao::saveLdapRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DatabaseRequestStage.class, dao::saveDatabaseRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MachineResourceUsage.class, dao::saveMachineResourceUsages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, LogEntry.class, dao::saveLogEntries), executor));
        return allOf(cf.toArray(CompletableFuture[]::new)).thenApply(v->
                cf.stream()
                    .map(CompletableFuture::join)
                    .flatMap(Collection::stream)
                    .toList()
        ).join();
    }

    private <U extends EventTrace> List<EventTrace> filterAndApply(Collection<EventTrace> c, Class<U> clazz, Consumer<List<U>> saveFn) {
        var list = c.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
        if(!list.isEmpty()) {
            log.debug("Tracing {} {}", list.size(), clazz.getSimpleName());
            try {
                saveFn.accept(list);
                list = Collections.emptyList();
            } catch (Exception e) {
                log.error("Error while saving {} {}, because {}: {}", list.size(), clazz.getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return (List<EventTrace>) list;
    }
}
