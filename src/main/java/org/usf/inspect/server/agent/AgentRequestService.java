package org.usf.inspect.server.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.erm.ExceptionCatalog;
import org.usf.inspect.server.erm.InspectStore;
import org.usf.inspect.server.erm.RestRequestCatalog;
import org.usf.inspect.server.erm.RestSessionCatalog;
import org.usf.inspect.server.model.wrapper.RestRequestWrapper;
import org.usf.inspect.server.service.RequestService;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.mvc.StoreManager;

import java.time.Instant;
import java.util.*;

import static org.usf.inspect.server.JsonUtils.fromJson;
import static org.usf.inspect.server.Utils.fromNullableTimestamp;
import static org.usf.jquery.core.Mappers.toListMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRequestService {

    public Map<String, Object> getRestSession(UUID id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        var restSession = store.restSession();
        var instance = store.instance();
        var v = new QueryComposer()
                .columns(
                        restSession.apiName(), restSession.method(),
                        restSession.path(), restSession.query(), restSession.media(), restSession.auth(), restSession.status(),
                        restSession.start(), restSession.end(), restSession.thread(), restSession.user(), restSession.userAgt(), restSession.cacheControl(),
                        instance.appName(), instance.os(), instance.re(), instance.environement(), instance.namespace(), instance.branch()
                )
                .joins(restSession.instance().getJoins())
                .criteria(restSession.id().eq(id).and(restSession.start().ge(instance.start())));
        return store.execute(v.compose(store), rs -> {
            if(rs.next()) {
                Map<String, Object> instanceMap = new LinkedHashMap<>();
                instanceMap.put("application_name", rs.getString("appName"));
                instanceMap.put("application_environment", rs.getString("environement"));
                instanceMap.put("application_branch", rs.getString("branch"));
                instanceMap.put("application_operating_system", rs.getString("os"));
                instanceMap.put("application_namespace", rs.getString("namespace"));
                instanceMap.put("application_repository", "dev/donnees/stm/produits/asm/" + rs.getString("appName"));
                instanceMap.put("application_jira", "N1T");

                Map<String, Object> sessionMap = new LinkedHashMap<>();
                sessionMap.put("rest_api_start", fromNullableTimestamp(rs.getTimestamp("start")));
                sessionMap.put("rest_api_end", fromNullableTimestamp(rs.getTimestamp("end")));
                sessionMap.put("rest_api_method", rs.getString("method"));
                sessionMap.put("rest_api_name", rs.getString("apiName"));
                sessionMap.put("rest_api_url", buildRestApiPathAndQuery(
                        rs.getString("path"), rs.getString("query")));
                sessionMap.put("rest_api_content_type", rs.getString("media"));
                sessionMap.put("rest_api_status", rs.getString("status"));
                sessionMap.put("rest_api_thread", rs.getString("thread"));
                sessionMap.put("rest_api_authentification_method", rs.getString("auth"));
                sessionMap.put("rest_api_user_agent", rs.getString("cacheControl"));
                sessionMap.put("rest_api_user", rs.getString("user"));
                sessionMap.put("rest_api_environment", instanceMap);
                return sessionMap;
            }
            return Map.of();
        });
    }

    public List<Map<String, Object>> getRestRequests(UUID id)  {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        var restRequest = store.restRequest();
        var v = new QueryComposer()
                .columns(
                        restRequest.id(), restRequest.auth(), restRequest.path(), restRequest.query(), restRequest.method(), restRequest.user(), restRequest.media(),
                        restRequest.media(), restRequest.status(), restRequest.start(), restRequest.end(), restRequest.thread()
                )
                .criteria(restRequest.parent().eq(id));
        return  store.execute(v.compose(store), toListMapper((rs, row) -> {
            Map<String, Object> mapRequest = new LinkedHashMap<>();
            mapRequest.put("request_id", rs.getString("id"));
            mapRequest.put("request_type", "HTTP");
            mapRequest.put("request_start", fromNullableTimestamp(rs.getTimestamp("start")));
            mapRequest.put("request_end", fromNullableTimestamp(rs.getTimestamp("end")));
            mapRequest.put("request_method", rs.getString("method"));
            mapRequest.put("request_url", buildRestApiPathAndQuery(
                    rs.getString("path"), rs.getString("query")));
            mapRequest.put("request_content_type", rs.getString("media"));
            mapRequest.put("request_status", rs.getString("status"));
            mapRequest.put("request_thread", rs.getString("thread"));
            mapRequest.put("request_user", rs.getString("user"));
            return mapRequest;
        }));
    }

    public List<Map<String, Object>> getDatabaseRequests(UUID id)  {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        var databaseRequest = store.databaseRequest();
        var v = new QueryComposer()
                .columns(
                        databaseRequest.id(), databaseRequest.user(), databaseRequest.db(),
                        databaseRequest.schema(), databaseRequest.start(), databaseRequest.end(), databaseRequest.thread()
                )
                .criteria(databaseRequest.parent().eq(id));
        return  store.execute(v.compose(store), toListMapper((rs, row) -> {
            Map<String, Object> mapRequest = new LinkedHashMap<>();
            mapRequest.put("request_id", rs.getString("id"));
            mapRequest.put("request_type", "JDBC");
            mapRequest.put("request_start", fromNullableTimestamp(rs.getTimestamp("start")));
            mapRequest.put("request_end", fromNullableTimestamp(rs.getTimestamp("end")));
            mapRequest.put("request_database_name", rs.getString("db"));
            mapRequest.put("request_schema_name", rs.getString("schema"));
            mapRequest.put("request_thread", rs.getString("thread"));
            mapRequest.put("request_user", rs.getString("user"));
            return mapRequest;
        }));
    }

    public List<Map<String, Object>> getExceptions(UUID id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        var exception = store.exception();
        var query = new QueryComposer()
                .columns(exception.errType(), exception.errMsg(),
                        exception.stacktrace(), exception.order(),
                        exception.parent())
                .criteria(exception.parent().eq(id));
        return store.execute(query.compose(store), toListMapper((rs, row) -> {
            Map<String, Object> mapException = new LinkedHashMap<>();
            mapException.put("type", rs.getString("errType"));
            mapException.put("message", rs.getString("errMsg"));
            mapException.put("stacktrace", fromJson(rs.getString("stacktrace"), StackTraceRow[].class));
            return mapException;
        }));
    }

    static String buildRestApiPathAndQuery(String path, String query) {
        String urlPath = path == null ? "" : path.trim();
        String urlQuery = query == null ? "" : query.trim();
        if (urlQuery.startsWith("?")) {
            return urlPath + urlQuery;
        }
        return urlQuery.isEmpty() ? urlPath : urlPath + "?" + urlQuery;
    }
}
