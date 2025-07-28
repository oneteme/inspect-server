package org.usf.inspect.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.AbstractRequest;
import org.usf.inspect.core.AbstractStage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static org.usf.inspect.core.SessionManager.nextId;

@Slf4j
public class RetroUtils {

    public static EventTrace[] toV4(Session[] sessions) {
        List<EventTrace> traces = new ArrayList<>();
        for(Session s : sessions) {
            if(isNull(s.getId())) {
                if(s instanceof MainSessionWrapper) {
                    s.setId(nextId()); // safe id set for web collectors
                }
                else if(s instanceof RestSessionWrapper) {
                    log.warn("RestSesstion.id is null : {}", s);
                }
            }
            toV4(s.getId(), s.getDatabaseRequests(), DatabaseRequestWrapper::getActions, traces::add);
            toV4(s.getId(), s.getFtpRequests(), FtpRequestWrapper::getActions, traces::add);
            toV4(s.getId(), s.getLdapRequests(), NamingRequestWrapper::getActions, traces::add);
            toV4(s.getId(), s.getMailRequests(), MailRequestWrapper::getActions, traces::add);
            toV4(s.getId(), s.getRestRequests(), (e) -> {
                var stage = new HttpRequestStage();
                stage.setName(HttpAction.PROCESS.name());
                stage.setStart(e.getStart());
                stage.setEnd(e.getEnd());
                if(e.getException() != null) {
                    if(e.getException().getType() == null){
                        e.setBodyContent(e.getException().getMessage());
                        stage.setException(null);
                    }else {
                        stage.setException(e.getException());
                    }
                }
                return List.of(stage);
            }, traces::add);
            toV4(s.getId(), s.getLocalRequests(), null, traces::add);
            traces.add(s);
        }
        return traces.toArray(EventTrace[]::new);
    }

    private static <T extends Wrapper<? extends AbstractRequest>> void toV4(String sessionId, Collection<T> requests, Function<T, List<? extends AbstractStage>> fn, Consumer<EventTrace> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var o : requests) {
                var req = o.unwrap();
                req.setSessionId(sessionId);
                req.setId(nextId());
                // Ajouter le is completed
                consumer.accept(req);
                if(fn != null) {
                    var inc = new AtomicInteger(0);
                    for(var stage : fn.apply(o)) {
                        stage.setRequestId(req.getId());
                        stage.setOrder(inc.incrementAndGet());
//                        if(stage.getException() != null) {
//                            stage.getException().setOrder(stage.getOrder());
//                        }
                        consumer.accept(stage);
                    }
                }
            }
        }
    }

    private static <T extends AbstractStage> boolean isCompleted(List<T> stage) {
        return stage == null || stage.stream().allMatch(a -> isNull(a.getException()));
    }
}

