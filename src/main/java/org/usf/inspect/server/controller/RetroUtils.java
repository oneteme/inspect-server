package org.usf.inspect.server.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.wrapper.Wrapper;
import org.usf.inspect.server.model.wrapper.MainSessionWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.*;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.HttpAction.PROCESS;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.core.SessionManager.nextId;
import static org.usf.inspect.server.Utils.isEmpty;

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
                var stage = createStage(restSession.getStart(), restSession.getEnd(), HttpSessionStage::new);
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
                if(nonNull(m.getActions())) {
                    var sa = m.getActions().stream().filter(a -> "SEND".equals(a.getName())).toList();
                    if(nonNull(m.getMails()) && m.getMails().size() == sa.size()){
                        for(var i = 0; i < m.getMails().size(); i++) {
                            sa.get(i).setMail(m.getMails().get(i));
                        }
                    }
                }
                return m.getActions();
            }, traces::add);
            toV4(s.getId(), s.getRestRequests(), (e) -> {
                e.setLinked(nonNull(e.getId()));

                var stage = createStage(e.getStart(), e.getEnd(), HttpRequestStage::new);
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
            toV4(s.getId(), s.getLocalRequests(), r-> Collections.emptyList(), traces::add);
        }
        return traces;
    }

    private static <T extends Wrapper<? extends AbstractRequest>, U extends AbstractStage> void toV4(String sessionId, Collection<T> requests, Function<T, List<U>> fn, Consumer<EventTrace> consumer) {
        if(requests != null && !requests.isEmpty()) {
            for(var o : requests) {
                var req = o.unwrap();
                req.setSessionId(sessionId);
                consumer.accept(req);
                var list = fn.apply(o);
                if(isNull(req.getId())){ //req.id = ses.id
                    req.setId(nextId());
                }
                var inc = 0;
                for(var s : list) {
                    s.setRequestId(req.getId());
                    s.setOrder(inc++);
                    consumer.accept(s);
                }
            }
        }
    }

    private static <T extends AbstractStage> T createStage(Instant start, Instant end, Supplier<T> supp) {
        var stg = supp.get();
        stg.setName(PROCESS.name());
        stg.setStart(start);
        stg.setEnd(end);
        return stg;
    }

    private static <T extends AbstractStage> boolean isFailed(List<T> stage) {
        return stage != null && stage.stream().anyMatch(a -> nonNull(a.getException()));
    }

    private static int mask(Session s) {
        var v = 0;
        if(!isEmpty(s.getLocalRequests())) {
            v |= LOCAL.getValue();
        }
        if(!isEmpty(s.getDatabaseRequests())) {
            v |= JDBC.getValue();
        }
        if(!isEmpty(s.getRestRequests())) {
            v |= REST.getValue();
        }
        if(!isEmpty(s.getFtpRequests())) {
            v |= FTP.getValue();
        }
        if(!isEmpty(s.getMailRequests())) {
            v |= SMTP.getValue();
        }
        if(!isEmpty(s.getLdapRequests())) {
            v |= LDAP.getValue();
        }
        return v;
    }
}

