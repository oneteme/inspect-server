package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.RequestMask;
import org.usf.inspect.core.StackTraceRow;
import org.usf.inspect.server.Utils;
import org.usf.inspect.server.dao.RequestDao;
import org.usf.inspect.server.erm.*;
import org.usf.inspect.server.model.Architecture;
import org.usf.inspect.server.model.RequestType;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.wrapper.*;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.mvc.StoreManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.sql.Timestamp.from;
import static java.util.UUID.fromString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.usf.inspect.core.ExecutorServiceWrapper.wrap;
import static org.usf.inspect.server.Utils.*;
import static org.usf.jquery.core.Join.innerJoin;
import static org.usf.jquery.core.Mappers.toListMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestDao dao;
    private final ExecutorService executorService = wrap(virtualThreadExecutor("inspect-tree", 10));

    public Session getMainTree(UUID id)  {
        var session = requireSingle(getMainSessions(id));
        if(session != null) {
            updateSessionsForTree(dao.selectChildsById(id, session.getStart()), session);
            return session;
        }
        throw new NoSuchElementException("no main session found");
    }

    public Session getRestTree(UUID id)  {
        var session = requireSingle(getRestSessions(Collections.singletonList(id),null));
        if(session != null) {
            updateSessionsForTree(dao.selectChildsById(id, session.getStart()), session);
            return session;
        }
        throw new NoSuchElementException("no rest session found");
    }

    public List<Architecture> createArchitecture(Instant start, Instant end, String[] env){
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        MainSessionCatalog mainSession = store.mainSession();
        RestSessionCatalog restSession = store.restSession();
        RestSessionCatalog restSessionFork = (RestSessionCatalog) store.restSession().mirror();
        RestRequestCatalog restRequest = store.restRequest();
        DatabaseRequestCatalog databaseRequest = store.databaseRequest();
        FtpRequestCatalog ftpRequest = store.ftpRequest();
        SmtpRequestCatalog smtpRequest = store.smtpRequest();
        LdapRequestCatalog ldapRequest = store.ldapRequest();
        InstanceCatalog instance = store.instance();
        InstanceCatalog instanceFork = (InstanceCatalog) store.instance().mirror();
        var q = new QueryComposer()
                .columns(
                        databaseRequest.db().as("name"), databaseRequest.schema().as("schema"),
                        instance.appName(),
                        Column.constant("JDBC").as("type"),
                        Column.constant("REST").as("source")
                )
                .distinct(true)
                .joins(restSession.instance().getJoins())
                .joins(restSession.databaseRequest().getJoins())         
                .criterias(
                        restSession.start().ge(from(start)),
                        restSession.end().lt(from(end)),
                        databaseRequest.db().notNull().or(databaseRequest.schema().notNull()),
                        databaseRequest.start().ge(from(start)),
                        instance.environement().in(env)
                );
        var q2 = new QueryComposer()
                .columns(
                        ftpRequest.host().as("name"), Column.constant("").as("schema"),
                        instance.appName(),
                        Column.constant("FTP").as("type"),
                        Column.constant("REST").as("source")
                )
                .distinct(true)
                .joins(restSession.instance().getJoins())
                .joins(restSession.ftpRequest().getJoins())
                .criterias(
                        restSession.start().ge(from(start)),
                        restSession.end().lt(from(end)),
                        ftpRequest.host().notNull(),
                        ftpRequest.start().ge(from(start)),
                        instance.environement().in(env)
                )
                .compose(store).asUnion(true);
        var q3 =  new QueryComposer()
                .columns(
                        smtpRequest.host().as("name"), Column.constant("").as("schema"),
                        instance.appName(),
                        Column.constant("SMTP").as("type"),
                        Column.constant("REST").as("source")
                )
                .distinct(true)
                .joins(restSession.instance().getJoins())
                .joins(restSession.smtpRequest().getJoins())
                .criterias(
                        restSession.start().ge(from(start)),
                        restSession.end().lt(from(end)),
                        smtpRequest.host().notNull(),
                        smtpRequest.start().ge(from(start)),
                        instance.environement().in(env)
                ).compose(store).asUnion(true);
        var q4 = new QueryComposer()
                .columns(
                        ldapRequest.host().as("name"), Column.constant("").as("schema"),
                        instance.appName(),
                        Column.constant("LDAP").as("type"),
                        Column.constant("REST").as("source")
                )
                .distinct(true)
                .joins(
                        restSession.instance().getJoins())
                .joins(
                        restSession.ldapRequest().getJoins()
                )
                .criterias(
                        restSession.start().ge(from(start)),
                        restSession.end().lt(from(end)),
                        ldapRequest.host().notNull(),
                        ldapRequest.start().ge(from(start)),
                        instance.environement().in(env)
                ).compose(store).asUnion(true);
        var q5 = new QueryComposer()
                .columns(
                        instanceFork.appName().as("name"), Column.constant("").as("schema"),
                        instance.appName(),
                        Column.constant("REST").as("type"),
                        Column.constant("REST").as("source")
                )
                .distinct(true)
                .joins(
                        innerJoin(restRequest.getView(), restSession.id().eq(restRequest.parent())),
                        innerJoin(instance.getView(), restSession.instanceEnv().eq(instance.id())),
                        innerJoin(restSessionFork.getView(), restRequest.id().eq(restSessionFork.id())),
                        innerJoin(instanceFork.getView(), restSessionFork.instanceEnv().eq(instanceFork.id()))
                )
                .criterias(
                        restSession.start().ge(from(start)),
                        restSession.end().lt(from(end)),
                        restRequest.start().ge(from(start)),
                        instance.environement().in(env)
                ).compose(store).asUnion(true);
        var q6 = new QueryComposer()
                .columns(
                        instanceFork.appName().as("name"), Column.constant("").as("schema"),
                        instance.appName(),
                        Column.constant("VIEW").as("type"),
                        mainSession.type().as("source")
                )
                .distinct(true)
                .joins(
                        innerJoin(restRequest.getView(), mainSession.id().eq(restRequest.parent())),
                        innerJoin(instance.getView(), mainSession.instanceEnv().eq(instance.id())),
                        innerJoin(restSession.getView(), restRequest.id().eq(restSession.id())),
                        innerJoin(instanceFork.getView(), restSession.instanceEnv().eq(instanceFork.id()))
                )
                .criterias(
                        mainSession.start().ge(from(start)),
                        mainSession.end().lt(from(end)),
                        mainSession.type().eq("VIEW"),
                        restRequest.start().ge(from(start)),
                        instance.environement().in(env)
                ).compose(store).asUnion(true);
        return store.execute(q.unions(q2, q3, q4, q5, q6).compose(store), rs -> {
            Map<String, List<Architecture>> map = new HashMap<>();
            Map<String, String> sourceMap = new HashMap<>();
            while(rs.next()) {
                var key = rs.getString("appName");
                var type = rs.getString("type");
                var source = rs.getString("source");
                if(!map.containsKey(key)) {
                    map.put(key, new ArrayList<>());
                    sourceMap.put(key, source);
                }
                map.get(key).add(new Architecture(rs.getString("name"), rs.getString("schema"), type, null));
            }
            return map.entrySet().stream().map(entry -> new Architecture(entry.getKey(), null, sourceMap.get(entry.getKey()), entry.getValue())).toList();
        });
    }

    public Map<String, String> getSessionParent(RequestType requestType2, String childId) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);

        record IdAndParent(ViewColumn id, ViewColumn parent) {}

        var cols = switch (requestType2) {
            case rest -> { var r = store.restRequest();      yield new IdAndParent(r.id(), r.parent()); }
            case jdbc -> { var r = store.databaseRequest();  yield new IdAndParent(r.id(), r.parent()); }
            case ftp  -> { var r = store.ftpRequest();       yield new IdAndParent(r.id(), r.parent()); }
            case smtp -> { var r = store.smtpRequest();      yield new IdAndParent(r.id(), r.parent()); }
            case ldap -> { var r = store.ldapRequest();      yield new IdAndParent(r.id(), r.parent()); }
            default   -> throw new IllegalArgumentException("Unsupported request type for parent lookup: " + requestType2);
        };

        String prnt = store.execute(
            new QueryComposer()
                .columns(cols.parent())
                .criteria(cols.id().eq(fromString(childId)))
                .compose(store),
            rs -> rs.next() ? rs.getString(cols.parent().getTag()) : null
        );

        if (prnt != null) {
            RestSessionCatalog restSession = store.restSession();
            String res = store.execute(
                new QueryComposer()
                    .columns(restSession.id())
                    .criteria(restSession.id().eq(fromString(prnt)))
                    .compose(store),
                rs -> rs.next() ? rs.getString(restSession.id().getTag()) : null
            );
            if (res != null) {
                return Map.of("id", res, "type", "rest");
            }

            MainSessionCatalog mainSession = store.mainSession();
            return store.execute(
                new QueryComposer()
                    .columns(mainSession.id(), mainSession.type())
                    .criteria(mainSession.id().eq(fromString(prnt)))
                    .compose(store),
                rs -> rs.next()
                    ? Map.of("id", rs.getString(mainSession.id().getTag()), "type", rs.getString(mainSession.type().getTag()))
                    : Collections.emptyMap()
            );
        }
        return Collections.emptyMap();
    }


    private void updateSessionsForTree(Collection<UUID> ids, Session parent)  {
        var start = parent.getStart();
        var sessions = Utils.isEmpty(ids) ? new ArrayList<Session>() : getRestSessions(ids, start);
        sessions.add(parent);
        var reqMap = sessions.stream().collect(toMap(Session::getId, identity()));

        // Déterminer les types de requêtes présents via le mask combiné
        var combinedMask = 0;
        for (var s : sessions) {
            combinedMask |= s.getRequestsMask();
        }
        var futures = new ArrayList<CompletableFuture<?>>();
        if (RequestMask.REST.is(combinedMask)) {
            futures.add(CompletableFuture.runAsync(() -> getRestRequestsCompleteForParent(reqMap.keySet(), start).forEach(r -> {
                reqMap.get(r.getSessionId()).getRestRequests().add(r);
                r.setRemoteTrace((RestSessionWrapper) reqMap.get(r.getId()));
            }), executorService));
        }
        if (RequestMask.FTP.is(combinedMask)) {
            futures.add(CompletableFuture.runAsync(() -> getFtpRequestsComplete(reqMap.keySet(), start).forEach(q -> reqMap.get(q.getSessionId()).getFtpRequests().add(q)), executorService));
        }
        if (RequestMask.SMTP.is(combinedMask)) {
            futures.add(CompletableFuture.runAsync(() -> getSmtpRequestsComplete(reqMap.keySet(), start).forEach(q -> reqMap.get(q.getSessionId()).getMailRequests().add(q)), executorService));
        }
        if (RequestMask.LDAP.is(combinedMask)) {
            futures.add(CompletableFuture.runAsync(() -> getLdapRequestsComplete(reqMap.keySet(), start).forEach(q -> reqMap.get(q.getSessionId()).getLdapRequests().add(q)), executorService));
        }
        if (RequestMask.JDBC.is(combinedMask)) {
            getDatabaseRequestsComplete(reqMap.keySet(), start).forEach(q -> reqMap.get(q.getSessionId()).getDatabaseRequests().add(q));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    private List<Session> getRestSessions(Collection<UUID> ids, Instant start)  { // remove if possible after optimizing tree
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RestSessionCatalog restSession = store.restSession();
        InstanceCatalog instance =  store.instance();
        var v = new QueryComposer()
                .columns(
                        restSession.id(), restSession.apiName(), restSession.method(), restSession.protocol(), restSession.host(), restSession.port(),
                        restSession.path(), restSession.query(), restSession.media(), restSession.auth(), restSession.status(), restSession.sizeIn(), restSession.sizeOut(),
                        restSession.contentEncodingIn(), restSession.contentEncodingOut(), restSession.start(), restSession.end(), restSession.thread(), restSession.errType(),
                        restSession.errMsg(), restSession.mask(), restSession.user(), restSession.userAgt(), restSession.cacheControl(), restSession.instanceEnv(),
                        instance.appName(), instance.os(), instance.re(), instance.address()
                )
                .joins(restSession.instance().getJoins())
                .criteria(restSession.id().in(ids.stream().toArray(UUID[]::new)).and(restSession.start().ge(instance.start())));
        if (start != null) {
            v.criteria(restSession.start().ge(from(start)));
        }
        return store.execute(v.compose(store), rs -> {
            List<Session> sessions = new ArrayList<>();
            while (rs.next()) {
                RestSessionWrapper session = new RestSessionWrapper();
                session.setId(rs.getObject("id", UUID.class));
                session.setMethod(rs.getString("method"));
                session.setProtocol(rs.getString("protocol"));
                session.setHost(rs.getString("host"));
                session.setPort(rs.getInt("port"));
                session.setPath(rs.getString("path"));
                session.setQuery(rs.getString("query"));
                session.setContentType((rs.getString("media")));
                session.setAuthScheme((rs.getString("auth")));
                session.setStatus(rs.getShort("status"));
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
                if(RequestMask.JDBC.is(session.getRequestsMask())) {
                    session.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(session.getRequestsMask())) {
                    session.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(session.getRequestsMask())) {
                    session.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(session.getRequestsMask())) {
                    session.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(session.getRequestsMask())) {
                    session.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(session.getRequestsMask())) {
                    session.setLdapRequests(new ArrayList<>());
                }
                sessions.add(session);
            }
            return sessions;
        });
    }

    private List<Session> getMainSessions(UUID id) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        MainSessionCatalog mainSession = store.mainSession();
        InstanceCatalog instance =  store.instance();
        var v = new QueryComposer()
                .columns(
                        mainSession.id(), mainSession.name(), mainSession.start(), mainSession.end(), mainSession.type(), mainSession.location(), mainSession.thread(),
                        mainSession.errType(), mainSession.errMsg(), mainSession.mask(), mainSession.user(), mainSession.instanceEnv(),
                        instance.appName(), instance.os(), instance.re(), instance.address()
                )
                .joins(mainSession.instance().getJoins())
                .criteria(mainSession.id().eq(id)).compose(store);

        return store.execute(v, rs -> {
            List<Session> sessions = new ArrayList<>();
            while(rs.next()) {
                MainSessionWrapper main = new MainSessionWrapper();
                main.setId(rs.getObject("id", java.util.UUID.class));
                main.setName(rs.getString("name"));
                main.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                main.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                main.setType(rs.getString("type"));
                main.setLocation(rs.getString("location"));
                main.setThreadName(rs.getString("thread"));
                main.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), null));
                main.setAppName(rs.getString("appName"));
                main.setOs(rs.getString("os"));
                main.setRe(rs.getString("re"));
                main.setAddress(rs.getString("address"));
                main.setUser(rs.getString("user"));
                main.setInstanceId(rs.getObject("instanceEnv", java.util.UUID.class));
                main.setRequestsMask(rs.getInt("mask"));
                if(RequestMask.JDBC.is(main.getRequestsMask())) {
                    main.setDatabaseRequests(new ArrayList<>());
                }
                if(RequestMask.LOCAL.is(main.getRequestsMask())) {
                    main.setLocalRequests(new ArrayList<>());
                }
                if(RequestMask.REST.is(main.getRequestsMask())) {
                    main.setRestRequests(new ArrayList<>());
                }
                if(RequestMask.FTP.is(main.getRequestsMask())) {
                    main.setFtpRequests(new ArrayList<>());
                }
                if(RequestMask.SMTP.is(main.getRequestsMask())) {
                    main.setMailRequests(new ArrayList<>());
                }
                if(RequestMask.LDAP.is(main.getRequestsMask())) {
                    main.setLdapRequests(new ArrayList<>());
                }
                sessions.add(main);
            }
            return sessions;
        });
    }

    private List<RestRequestWrapper> getRestRequestsCompleteForParent(Collection<UUID> ids, Instant start)  { //use criteria
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RestRequestCatalog restRequest = store.restRequest();
        ExceptionCatalog exception =  store.exception();

        var v = new QueryComposer()
                .columns(
                        restRequest.id(), restRequest.protocol(), restRequest.auth(), restRequest.host(), restRequest.port(), restRequest.path(), restRequest.query(), restRequest.method(),
                        restRequest.status(), restRequest.sizeIn(), restRequest.sizeOut(), restRequest.contentEncodingIn(), restRequest.contentEncodingOut(), restRequest.start(), restRequest.end(), restRequest.thread(),
                        restRequest.linked(), restRequest.parent(),
                        exception.errType(), exception.errMsg()
                )
                .joins(restRequest.exception().getJoins())
                .criteria(restRequest.parent().in(ids.stream().map(UUID::toString).toArray()));
        if(start != null) {
            v.criteria(restRequest.start().ge(start));
        }
        v.order(restRequest.start().order());
        return  store.execute(v.compose(store), rs -> {
            List<RestRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                RestRequestWrapper out = new RestRequestWrapper();
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setId(rs.getObject("id", UUID.class));
                out.setProtocol(rs.getString("protocol"));
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setPath(rs.getString("path"));
                out.setQuery(rs.getString("query"));
                out.setMethod(rs.getString("method"));
                out.setStatus(rs.getShort("status"));
                out.setInDataSize(rs.getLong("sizeIn"));
                out.setOutDataSize(rs.getLong("sizeOut"));
                out.setInContentEncoding(rs.getString("contentEncodingIn"));
                out.setOutContentEncoding(rs.getString("contentEncodingOut"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setThreadName(rs.getString("thread"));
                out.setLinked(rs.getBoolean("linked"));
                out.setAuthScheme(rs.getString("auth"));
                out.setException(getExceptionInfoIfNotNull(rs.getString("errType"), rs.getString("errMsg"), null));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<DatabaseRequestWrapper> getDatabaseRequestsComplete(Collection<UUID> ids, Instant start)  {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        DatabaseRequestCatalog databaseRequest = store.databaseRequest();

        var v = new QueryComposer()
                .columns(
                        databaseRequest.id(), databaseRequest.host(), databaseRequest.port(), databaseRequest.db(), databaseRequest.start(), databaseRequest.end(),
                        databaseRequest.user(), databaseRequest.thread(), databaseRequest.driver(), databaseRequest.dbName(), databaseRequest.dbVersion(), databaseRequest.command(),
                        databaseRequest.failed(), databaseRequest.schema(), databaseRequest.parent()
                )
                .criteria(databaseRequest.parent().in(ids.stream().toArray(UUID[]::new)));
        if (start != null) {
            v.criteria(databaseRequest.start().ge(from(start)));
        }
        v.orders(databaseRequest.start().order());
        return  store.execute(v.compose(store), rs -> {
            List<DatabaseRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DatabaseRequestWrapper out = new DatabaseRequestWrapper();
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setId(rs.getObject("id", UUID.class));
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setName(rs.getString("db"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setUser(rs.getString("user"));
                out.setThreadName(rs.getString("thread"));
                out.setDriverVersion(rs.getString("driver"));
                out.setProductName(rs.getString("dbName"));
                out.setProductVersion(rs.getString("dbVersion"));
                out.setActions(new ArrayList<>());
                out.setCommand(rs.getString("command"));
                out.setFailed(rs.getBoolean("failed"));
                out.setSchema(rs.getString("schema"));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<FtpRequestWrapper> getFtpRequestsComplete(Collection<UUID> ids, Instant start) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        FtpRequestCatalog ftpRequest = store.ftpRequest();

        var v = new QueryComposer()
                .columns(
                        ftpRequest.id(), ftpRequest.host(), ftpRequest.port(), ftpRequest.protocol(), ftpRequest.serverVersion(), ftpRequest.clientVersion(),
                        ftpRequest.start(), ftpRequest.end(), ftpRequest.command(), ftpRequest.user(), ftpRequest.thread(), ftpRequest.failed(), ftpRequest.parent()
                )
                .criteria(ftpRequest.parent().in(ids.stream().toArray(UUID[]::new)));
        if (start != null) {
            v.criteria(ftpRequest.start().ge(from(start)));
        }
        v.orders(ftpRequest.start().order());
        return store.execute(v.compose(store), rs -> {
            List<FtpRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                FtpRequestWrapper out = new FtpRequestWrapper();
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setId(rs.getObject("id", UUID.class));
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setProtocol(rs.getString("protocol"));
                out.setServerVersion(rs.getString("serverVersion"));
                out.setClientVersion(rs.getString("clientVersion"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setCommand(rs.getString("command"));
                out.setUser(rs.getString("user"));
                out.setThreadName(rs.getString("thread"));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean("failed"));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<MailRequestWrapper> getSmtpRequestsComplete(Collection<UUID> ids, Instant start) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        SmtpRequestCatalog smtpRequest = store.smtpRequest();

        var v = new QueryComposer()
                .columns(
                        smtpRequest.id(), smtpRequest.host(), smtpRequest.port(), smtpRequest.start(), smtpRequest.end(), smtpRequest.command(), smtpRequest.user(), smtpRequest.thread(), smtpRequest.failed(), smtpRequest.parent()
                )
                .criteria(smtpRequest.parent().in(ids.stream().toArray(UUID[]::new)));
        if (start != null) {
            v.criteria(smtpRequest.start().ge(start));
        }
        v.orders(smtpRequest.start().order());
        return store.execute(v.compose(store), rs -> {
            List<MailRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                MailRequestWrapper out = new MailRequestWrapper();
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setId(rs.getObject("id", UUID.class));
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setCommand(rs.getString("command"));
                out.setUser(rs.getString("user"));
                out.setThreadName(rs.getString("thread"));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean("failed"));
                outs.add(out);
            }
            return outs;
        });
    }

    private List<DirectoryRequestWrapper> getLdapRequestsComplete(Collection<UUID> ids, Instant start) {
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        LdapRequestCatalog ldapRequest = store.ldapRequest();

        var v = new QueryComposer()
                .columns(
                        ldapRequest.id(), ldapRequest.host(), ldapRequest.port(), ldapRequest.protocol(), ldapRequest.start(), ldapRequest.end(), ldapRequest.command(), ldapRequest.user(), ldapRequest.thread(), ldapRequest.failed(), ldapRequest.parent()
                )
                .criteria(ldapRequest.parent().in(ids.stream().toArray(UUID[]::new)));
        if (start != null) {
            v.criteria(ldapRequest.start().ge(start));
        }
        v.orders(ldapRequest.start().order());
        return store.execute(v.compose(store), rs -> {
            List<DirectoryRequestWrapper> outs = new ArrayList<>();
            while (rs.next()) {
                DirectoryRequestWrapper out = new DirectoryRequestWrapper();
                out.setSessionId(rs.getObject("parent", UUID.class));
                out.setId(rs.getObject("id", UUID.class));
                out.setHost(rs.getString("host"));
                out.setPort(rs.getInt("port"));
                out.setProtocol(rs.getString("protocol"));
                out.setStart(fromNullableTimestamp(rs.getTimestamp("start")));
                out.setEnd(fromNullableTimestamp(rs.getTimestamp("end")));
                out.setCommand(rs.getString("command"));
                out.setUser(rs.getString("user"));
                out.setThreadName(rs.getString("thread"));
                out.setActions(new ArrayList<>());
                out.setFailed(rs.getBoolean("failed"));
                outs.add(out);
            }
            return outs;
        });
    }

    public Collection<String> getRequestHosts(RequestType requestType, String environment, Instant start, Instant end){
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        RequestCatalog request = requestType.getColFn().apply(store);
        InstanceCatalog instance = store.instance();
        var v = new QueryComposer()
                .distinct(true)
                .columns(request.host())
                .joins(request.instance().getJoins())
                .criterias(
                        request.start().ge(start).and(request.start().lt(end)),
                        instance.environement().eq(environment)
                )
                .order(request.host().order());
        return store.execute(v.compose(store), toListMapper((rs, row) -> rs.getString("host")));
    }

    public Collection<String> getRequestSchema(String environment, Instant start, Instant end, String host){
        InspectStore store = StoreManager.getInstance().getStore(InspectStore.class);
        DatabaseRequestCatalog databaseRequest = store.databaseRequest();
        InstanceCatalog instance = store.instance();

        var v = new QueryComposer()
                .distinct(true)
                .columns(databaseRequest.schema())
                .joins(databaseRequest.instance().getJoins())
                .criterias(
                        databaseRequest.start().ge(start).and(databaseRequest.start().lt(end)).and(databaseRequest.host().eq(host)),
                        instance.environement().eq(environment)
                );
        return store.execute(v.compose(store), toListMapper((rs, row) -> rs.getString("schema")));
    }

    public static ExceptionTrace getExceptionInfoIfNotNull(String className, String message, StackTraceRow[] stackTraceRows) {
        if(className != null || message != null) {
            return new ExceptionTrace(className, message, stackTraceRows, null);
        }
        return null;
    }
}
