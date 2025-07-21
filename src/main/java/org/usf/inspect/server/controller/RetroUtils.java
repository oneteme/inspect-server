package org.usf.inspect.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.SessionManager.nextId;

@Slf4j
public class RetroUtils {

    public static void toV4(String instanceId, Session[] sessions, Consumer<EventTrace> consumer) {
        for(Session s : sessions) {
            s.setInstanceId(instanceId);
            if(isNull(s.getId())) {
                if(s instanceof MainSession) {
                    s.setId(nextId()); // safe id set for web collectors
                }
                else if(s instanceof RestSession) {
                    log.warn("RestSesstion.id is null : {}", s);
                }
            }
            toV4(instanceId,s.getId(), s.getDatabaseRequests(), DatabaseRequest::getActions, consumer);
            toV4(instanceId,s.getId(), s.getFtpRequests(), FtpRequest::getActions, consumer);
            toV4(instanceId,s.getId(), s.getLdapRequests(), NamingRequest::getActions, consumer);
            toV4(instanceId,s.getId(), s.getMailRequests(), MailRequest::getActions, consumer);
            toV4(instanceId,s.getId(), s.getRestRequests(), (e) -> {
                var stage = new HttpRequestStage();
                stage.setStart(e.getStart());
                stage.setEnd(e.getEnd());
                if(e.getException() != null) {
                    if(e.getException().getType()== null){
                        e.setBodyContent(e.getException().getMessage());
                        stage.setException(null);
                    }else {
                        e.getException().setIdRequest(e.getIdRequest());
                        stage.setException(e.getException());
                    }
                }
                return List.of(stage);
            }, consumer);
            toV4(instanceId,s.getId(), s.getLocalRequests(), null, consumer);
            consumer.accept(s);
        }
    }

    private static <T extends SessionStage> void toV4(String instanceId, String sessionId, Collection<T> requests, Function<T, List<? extends  RequestStage>> fn, Consumer<EventTrace> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var req : requests) {
                req.setCdSession(sessionId);
                req.setIdRequest(nextId());
                req.setInstanceId(instanceId);
                // Ajouter le is completed
                consumer.accept(req);
                if(fn != null) {
                    var inc = new AtomicInteger(0);
                    for(var stage : fn.apply(req)) {
                        stage.setIdRequest(req.getIdRequest());
                        stage.setOrder(inc.incrementAndGet());
                        if(stage.getException() != null) {
                            stage.getException().setOrder(stage.getOrder());
                        }
                        consumer.accept(stage);
                    }
                }
            }
        }
    }

    private static <T extends RequestStage> boolean isCompleted(List<T> stage) {
        return stage == null || stage.stream().allMatch(a -> isNull(a.getException()));
    }
}

