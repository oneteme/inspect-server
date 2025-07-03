package org.usf.inspect.server.controller.lazy;

import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.server.model.lazy.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.Session.nextId;

@Slf4j
public class RetroUtils {

    public static void toV4(String instanceId, Session[] sessions, Consumer<Metric> consumer) {
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
            toV4(s.getId(), s.getDatabaseRequests(), DatabaseRequest::getActions, consumer);
            toV4(s.getId(), s.getFtpRequests(), FtpRequest::getActions, consumer);
            toV4(s.getId(), s.getLdapRequests(), NamingRequest::getActions, consumer);
            toV4(s.getId(), s.getMailRequests(), MailRequest::getActions, consumer);
            toV4(s.getId(), s.getRestRequests(), (e) -> {
                var stage = new HttpRequestStage();
                stage.setStart(e.getStart());
                stage.setEnd(e.getEnd());
                if(e.getException() != null) stage.setException(e.getException());
                return List.of(stage);
            }, consumer);
            toV4(s.getId(), s.getLocalRequests(), null, consumer);
        }
    }

    private static <T extends SessionStage> void toV4(String sessionId, Collection<T> requests, Function<T, List<? extends  RequestStage>> fn, Consumer<Metric> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var req : requests) {
                req.setCdSession(sessionId);
                req.setIdRequest(nextId());
                // Ajouter le is completed
                consumer.accept(req);
                if(fn != null) {
                    var inc = new AtomicInteger(0);
                    for(var stage : fn.apply(req)) {
                        stage.setIdRequest(req.getIdRequest());
                        stage.setOrder(inc.incrementAndGet());
                        // Ajouter id de l'exception
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

