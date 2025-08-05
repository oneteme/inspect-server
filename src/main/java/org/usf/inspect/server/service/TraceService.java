package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.dao.NewRequestDao;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.wrapper.*;

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
    private final NewRequestDao dao;
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
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MainSessionWrapper.class, dao::saveMainSessions), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, RestSessionWrapper.class, dao::saveRestSessions), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, RestRequestWrapper.class, dao::saveRestRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, LocalRequestWrapper.class, dao::saveLocalRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MailRequestWrapper.class, dao::saveMailRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, FtpRequestWrapper.class, dao::saveFtpRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DirectoryRequestWrapper.class, dao::saveLdapRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DatabaseRequestWrapper.class, dao::saveDatabaseRequests), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, HttpRequestStageWrapper.class, dao::saveHttpRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, HttpSessionStageWrapper.class, dao::saveHttpSessionStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MailRequestStageWrapper.class, dao::saveMailRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, FtpRequestStageWrapper.class, dao::saveFtpRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DirectoryRequestStageWrapper.class, dao::saveLdapRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, DatabaseRequestStageWrapper.class, dao::saveDatabaseRequestStages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, MachineResourceUsageWrapper.class, dao::saveMachineResourceUsages), executor));
        cf.add(supplyAsync(() -> filterAndApply(eventTraces, LogEntryWrapper.class, dao::saveLogEntries), executor));
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
        log.debug("tracing {} {}", list.size(), clazz.getSimpleName());
        if(!list.isEmpty()) {
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
