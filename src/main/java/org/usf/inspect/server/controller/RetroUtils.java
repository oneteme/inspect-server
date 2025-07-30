package org.usf.inspect.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.AbstractRequest;
import org.usf.inspect.core.AbstractStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.Wrapper;
import org.usf.inspect.server.model.wrapper.HttpRequestStageWrapper;
import org.usf.inspect.server.model.wrapper.HttpSessionStageWrapper;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

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
            toV4(s.getId(), s.getDatabaseRequests(), d -> {
                d.setCommand(d.mainCommand());
                d.setFailed(!isCompleted(d.getActions()));
                return d.getActions();
            }, traces::add);
            toV4(s.getId(), s.getFtpRequests(), f -> {
                f.setFailed(!isCompleted(f.getActions()));
                return f.getActions();
            }, traces::add);
            toV4(s.getId(), s.getLdapRequests(), n -> {
                n.setFailed(!isCompleted(n.getActions()));
                return n.getActions();
            }, traces::add);
            toV4(s.getId(), s.getMailRequests(), m -> {
                m.setFailed(!isCompleted(m.getActions()));
                return m.getActions();
            }, traces::add);
            toV4(s.getId(), s.getRestRequests(), (e) -> {
                var stage = new HttpRequestStageWrapper();
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
            var stage = new HttpSessionStageWrapper();
            stage.setName(HttpAction.PROCESS.name());
            stage.setStart(s.getStart());
            stage.setEnd(s.getEnd());
            stage.setRequestId(s.getId());
            stage.setOrder(1);
            traces.add(stage);
            traces.add(s);
        }
        return traces.toArray(EventTrace[]::new);
    }

    private static <T extends Wrapper<? extends AbstractRequest> & EventTrace, U extends  Wrapper<? extends AbstractStage> & EventTrace> void toV4(String sessionId, Collection<T> requests, Function<T, List<U>> fn, Consumer<EventTrace> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var o : requests) {
                var req = o.unwrap();
                req.setSessionId(sessionId);
                req.setId(nextId());
                // Ajouter le is completed
                consumer.accept(o);
                if(fn != null) {
                    var inc = new AtomicInteger(0);
                    for(var s : fn.apply(o)) {
                        var stage = s.unwrap();
                        stage.setRequestId(req.getId());
                        stage.setOrder(inc.incrementAndGet());
//                        if(stage.getException() != null) {
//                            stage.getException().setOrder(stage.getOrder());
//                        }
                        consumer.accept(s);
                    }
                }
            }
        }
    }

    private static <T extends Wrapper<? extends AbstractStage>> boolean isCompleted(List<T> stage) {
        return stage == null || stage.stream().allMatch(a -> isNull(a.unwrap().getException()));
    }
}

