package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.CompletableMetric;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestSession;
import org.usf.inspect.server.model.Session;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class RestSessionWrapper implements Session {
    private final RestSession restSession = new RestSession();

    @Deprecated(since = "v1.1", forRemoval = true)
    private List<RestRequestWrapper> restRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<DatabaseRequestWrapper> databaseRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<LocalRequestWrapper> localRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<FtpRequestWrapper> ftpRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<MailRequestWrapper> mailRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<DirectoryRequestWrapper> ldapRequests;

    private String instanceId;
    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;

    public void setRequestsMask(int requestsMask) {
        restSession.setRequestsMask(requestsMask);
    }

    public int getRequestsMask() {
        return restSession.getRequestsMask();
    }

    public void setException(ExceptionInfo exception) {
        restSession.setException(exception);
    }

    public ExceptionInfo getException() {
        return restSession.getException();
    }

    public void setURI(URI uri) {
        restSession.setURI(uri);
    }

    public String getSessionId() {
        return restSession.getSessionId();
    }

    public String getInContentEncoding() {
        return restSession.getInContentEncoding();
    }

    public void setPath(String path) {
        restSession.setPath(path);
    }

    public void setMethod(String method) {
        restSession.setMethod(method);
    }

    public String getMethod() {
        return restSession.getMethod();
    }

    public String getThreadName() {
        return restSession.getThreadName();
    }

    public void setQuery(String query) {
        restSession.setQuery(query);
    }

    public void setBodyContent(String bodyContent) {
        restSession.setBodyContent(bodyContent);
    }

    public void setSessionId(String sessionId) {
        restSession.setSessionId(sessionId);
    }

    public void setAuthScheme(String authScheme) {
        restSession.setAuthScheme(authScheme);
    }

    public String getProtocol() {
        return restSession.getProtocol();
    }

    public void setThreadName(String threadName) {
        restSession.setThreadName(threadName);
    }

    public String getAuthScheme() {
        return restSession.getAuthScheme();
    }

    public long getInDataSize() {
        return restSession.getInDataSize();
    }

    public void setPort(int port) {
        restSession.setPort(port);
    }

    public String getUser() {
        return restSession.getUser();
    }

    public void setOutDataSize(long outDataSize) {
        restSession.setOutDataSize(outDataSize);
    }

    public String getBodyContent() {
        return restSession.getBodyContent();
    }

    public void setStart(Instant start) {
        restSession.setStart(start);
    }

    public String getHost() {
        return restSession.getHost();
    }

    public String getOutContentEncoding() {
        return restSession.getOutContentEncoding();
    }

    public void setInDataSize(long inDataSize) {
        restSession.setInDataSize(inDataSize);
    }

    public void setOutContentEncoding(String outContentEncoding) {
        restSession.setOutContentEncoding(outContentEncoding);
    }

    public void setUser(String user) {
        restSession.setUser(user);
    }

    public long getOutDataSize() {
        return restSession.getOutDataSize();
    }

    public void setInContentEncoding(String inContentEncoding) {
        restSession.setInContentEncoding(inContentEncoding);
    }

    public void setEnd(Instant end) {
        restSession.setEnd(end);
    }

    public int getPort() {
        return restSession.getPort();
    }

    public void setProtocol(String protocol) {
        restSession.setProtocol(protocol);
    }

    public void setContentType(String contentType) {
        restSession.setContentType(contentType);
    }

    public void setStatus(int status) {
        restSession.setStatus(status);
    }

    public void setHost(String host) {
        restSession.setHost(host);
    }

    public String getQuery() {
        return restSession.getQuery();
    }

    public String getPath() {
        return restSession.getPath();
    }

    public String getContentType() {
        return restSession.getContentType();
    }

    public int getStatus() {
        return restSession.getStatus();
    }

    public boolean wasCompleted() {
        return restSession.wasCompleted();
    }

    public String getId() {
        return restSession.getId();
    }

    public void setId(String id) {
        restSession.setId(id);
    }

    public Instant getEnd() {
        return restSession.getEnd();
    }

    public Instant getStart() {
        return restSession.getStart();
    }

    public void setName(String name) {
        restSession.setName(name);
    }

    public void setUserAgent(String userAgent) {
        restSession.setUserAgent(userAgent);
    }

    public void setCacheControl(String cacheControl) {
        restSession.setCacheControl(cacheControl);
    }

    public String getCacheControl() {
        return restSession.getCacheControl();
    }

    public String getUserAgent() {
        return restSession.getUserAgent();
    }

    public String getName() {
        return restSession.getName();
    }

    @Override
    public CompletableMetric copy() {
        throw new UnsupportedOperationException("Copying of RestSessionWrapper is not supported");
    }
}
