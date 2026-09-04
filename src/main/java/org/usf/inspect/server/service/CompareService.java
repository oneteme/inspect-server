package org.usf.inspect.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.server.erm.*;
import org.usf.inspect.server.model.wrapper.RestRequestWrapper;
import org.usf.inspect.server.model.wrapper.RestSessionWrapper;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.mvc.StoreManager;

import java.util.*;

import static java.util.UUID.*;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.inspect.server.Utils.requireSingle;
import static org.usf.inspect.server.mapper.Mappers.getExceptionInfoIfNotNull;
import static org.usf.jquery.core.Join.innerJoin;

@Slf4j
@Service
public class CompareService {

    public Map<String, Object> getComparedSession(String id) {
        var request = getRestRequestById(id);
        var result = new HashMap<String, Object>();
        if (request != null) {
            var name = getApiNameById(id);
            if (name != null) {
                request.setName(name);
            }
            result.put("request", request);
        }
        var session = getRestSessionById(id);
        if (session != null) {
            result.put("session", getRestSessionById(id));
        }

        if(result.isEmpty()) {
            throw new NoSuchElementException("neither session nor request found");
        }
        return result;
    }

    private String getApiNameById(String id) {
        var r = getApiNameRestSessionById(id);
        if (r != null) {
            return r;
        }
        return getApiNameMainSessionById(id);
    }

    private String getApiNameRestSessionById(String id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RestSessionCatalog restSession = store.restSession();
        RestRequestCatalog restRequest = store.restRequest();
        var v = new QueryComposer()
                .columns(restSession.apiName())
                .joins(restSession.restRequest().getJoins())
                .criterias(restRequest.id().eq(fromString(id)));
        return store.execute(v.compose(store), rs -> rs.next() ? rs.getString("apiName") : null);
    }

    private String getApiNameMainSessionById(String id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        MainSessionCatalog mainSession = store.mainSession();
        RestRequestCatalog restRequest = store.restRequest();
        var v = new QueryComposer()
                .columns(mainSession.name())
                .joins(mainSession.restRequest().getJoins())
                .criterias(restRequest.id().eq(fromString(id)));
        return store.execute(v.compose(store), rs -> rs.next() ? rs.getString("name") : null);
    }

    private RestSessionWrapper getRestSessionById(String id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RestSessionCatalog restSession = store.restSession();
        InstanceCatalog instance = store.instance();
        var v = new QueryComposer()
                .columns(restSession.id(), restSession.apiName(), restSession.method(),
                        restSession.protocol(), restSession.host(), restSession.port(), restSession.path(), restSession.query(),
                        restSession.media(), restSession.auth(), restSession.status(), restSession.sizeIn(), restSession.sizeOut(),
                        restSession.contentEncodingIn(), restSession.contentEncodingOut(), restSession.start(), restSession.end(), restSession.thread(),
                        restSession.errType(), restSession.errMsg(), restSession.mask(), restSession.user(), restSession.cacheControl(), restSession.userAgt(), restSession.instanceEnv(),
                        instance.appName(), instance.os(), instance.re(), instance.address(), instance.branch(), instance.hash(), instance.environement(), instance.version())
                .joins(innerJoin(instance.getView(), restSession.instanceEnv().eq(instance.id()).and(restSession.start().ge(instance.start()))))
                .criterias(restSession.id().eq(fromString(id)));
        return requireSingle(store.execute(v.compose(store), rs -> {
            var sessions = new ArrayList<RestSessionWrapper>();
            while (rs.next()) {
                var session = new RestSessionWrapper();
                session.setId(rs.getObject("id", UUID.class));
                session.setMethod(rs.getString("method"));
                session.setProtocol(rs.getString("protocol"));
                session.setHost(rs.getString("host"));
                session.setPort(rs.getInt("port"));
                session.setPath(rs.getString("path"));
                session.setQuery(rs.getString("query"));
                session.setContentType(rs.getString("media"));
                session.setAuthScheme(rs.getString("auth"));
                session.setStatus(rs.getInt("status"));
                session.setInDataSize(rs.getLong("sizeIn"));
                session.setOutDataSize(rs.getLong("sizeOut"));
                session.setInContentEncoding(rs.getString("contentEncodingIn"));
                session.setOutContentEncoding(rs.getString("contentEncodingOut"));
                session.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                session.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                session.setThreadName(rs.getString("thread"));
                session.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), null));
                session.setName(rs.getString("apiName"));
                session.setUserAgent(rs.getString("userAgt"));
                session.setUser(rs.getString("user"));
                session.setInstanceId(rs.getObject("instanceEnv", UUID.class));
                session.setCacheControl(rs.getString("cacheControl"));
                session.setOs(rs.getString("os"));
                session.setRe(rs.getString("re"));
                session.setAddress(rs.getString("address"));
                session.setAppName(rs.getString("appName"));
                session.setRequestsMask(rs.getInt("mask"));
                session.setBranch(rs.getString("branch"));
                session.setHash(rs.getString("hash"));
                session.setEnvironment(rs.getString("environement"));
                session.setVersion(rs.getString("version"));
                sessions.add(session);
            }
            return sessions;
        }));
    }

    private RestRequestWrapper getRestRequestById(String id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RestRequestCatalog restRequest = store.restRequest();
        InstanceCatalog instance = store.instance();
        ExceptionCatalog exception = store.exception();
        var v = new QueryComposer()
                .columns(restRequest.id(), restRequest.protocol(), restRequest.auth(), restRequest.host(),
                        restRequest.port(), restRequest.path(), restRequest.query(), restRequest.method(), restRequest.status(),
                        restRequest.sizeIn(), restRequest.sizeOut(), restRequest.contentEncodingIn(), restRequest.contentEncodingOut(),
                        restRequest.start(), restRequest.end(), restRequest.thread(), restRequest.bodyContent(), restRequest.user(), restRequest.linked(), restRequest.parent(),
                        instance.appName(), instance.os(), instance.re(), instance.address(), instance.branch(), instance.hash(), instance.version(), instance.environement(),
                        exception.errType(), exception.errMsg())
                .joins(restRequest.exception().getJoins())
                .join(innerJoin(instance.getView(), restRequest.instanceEnv().eq(instance.id()).and(restRequest.start().ge(instance.start()))))
                .criterias(restRequest.id().eq(fromString(id)));
        return store.execute(v.compose(store), rs -> {
            if(rs.next()) {
                var request = new RestRequestWrapper();
                request.setSessionId(rs.getObject("parent", UUID.class));
                request.setId(rs.getObject("id", UUID.class));
                request.setProtocol(rs.getString("protocol"));
                request.setHost(rs.getString("host"));
                request.setPort(rs.getInt("port"));
                request.setPath(rs.getString("path"));
                request.setQuery(rs.getString("query"));
                request.setMethod(rs.getString("method"));
                request.setStatus(rs.getInt("status"));
                request.setInDataSize(rs.getLong("sizeIn"));
                request.setOutDataSize(rs.getLong("sizeOut"));
                request.setInContentEncoding(rs.getString("contentEncodingIn"));
                request.setOutContentEncoding(rs.getString("contentEncodingOut"));
                request.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                request.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                request.setThreadName(rs.getString("thread"));
                request.setLinked(rs.getBoolean("linked"));
                request.setAuthScheme(rs.getString("auth"));
                request.setOs(rs.getString("os"));
                request.setRe(rs.getString("re"));
                request.setAddress(rs.getString("address"));
                request.setAppName(rs.getString("appName"));
                request.setBodyContent(rs.getString("bodyContent"));
                request.setBranch(rs.getString("branch"));
                request.setHash(rs.getString("hash"));
                request.setEnvironment(rs.getString("environement"));
                request.setVersion(rs.getString("version"));
                request.setUser(rs.getString("user"));
                request.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), null));

                if(rs.next()) {
                    log.warn("multiple rest request error found for id: {}", id);
                }
                return request;
            }
            return null;
        });
    }
}
