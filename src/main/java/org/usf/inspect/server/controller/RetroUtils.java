package org.usf.inspect.server.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.AbstractRequest;
import org.usf.inspect.core.AbstractStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.HttpAction;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.Wrapper;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.HttpAction.*;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.inspect.server.model.RequestMask.mask;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RetroUtils {

    public static List<EventTrace> toV4(Session[] sessions) {
        List<EventTrace> traces = new ArrayList<>();
        for(Session s : sessions) {
            if(s instanceof MainSessionWrapper ms) {
                if(isNull(ms.getId())) {
                    ms.setId(nextId());
                }
                var mainSession = ms.unwrap();
                mainSession.setRequestsMask(mask(ms));
                traces.add(mainSession);
            } else if (s instanceof RestSessionWrapper rs) {
                if(isNull(rs.getId())) {
                    log.warn("RestSesstion.id is null : {}", rs);
                }
                var restSession = rs.unwrap();
                restSession.setRequestsMask(mask(rs));
                var stage = restSession.createStage(PROCESS, restSession.getStart(), restSession.getEnd(), null);
                stage.setRequestId(rs.getId());
                stage.setOrder(0);
                traces.add(stage);
                traces.add(restSession);
            }
            toV4(s.getId(), s.getDatabaseRequests(), d -> {
                d.setCommand(d.mainCommand());
                d.setFailed(isFailed(d.getActions()));
                return d.getActions();
            }, traces::add);
            toV4(s.getId(), s.getFtpRequests(), f -> {
                f.setFailed(isFailed(f.getActions()));
                return f.getActions();
            }, traces::add);
            toV4(s.getId(), s.getLdapRequests(), n -> {
                n.setFailed(isFailed(n.getActions()));
                return n.getActions();
            }, traces::add);
            toV4(s.getId(), s.getMailRequests(), m -> {
                m.setFailed(isFailed(m.getActions()));
                return m.getActions();
            }, traces::add);
            toV4(s.getId(), s.getRestRequests(), (e) -> {
                var stage = e.createStage(PROCESS, e.getStart(), e.getEnd(), null);
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
        }
        return traces;
    }

    private static <T extends Wrapper<? extends AbstractRequest>, U extends AbstractStage> void toV4(String sessionId, Collection<T> requests, Function<T, List<U>> fn, Consumer<EventTrace> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var o : requests) {
                var req = o.unwrap();
                req.setSessionId(sessionId);
                if(isNull(req.getId())){ //req.id = ses.id
                    req.setId(nextId());
                }
                consumer.accept(req);
                if(fn != null) {
                    var inc = 0;
                    for(var s : fn.apply(o)) {
                        s.setRequestId(req.getId());
                        s.setOrder(inc++);
                        consumer.accept(s);
                    }
                }
            }
        }
    }

    private static <T extends AbstractStage> boolean isFailed(List<T> stage) {
        return stage != null && stage.stream().anyMatch(a -> nonNull(a.getException()));
    }
}

