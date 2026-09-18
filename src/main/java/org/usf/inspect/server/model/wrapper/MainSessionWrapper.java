package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;

import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.server.model.CompletableMetric;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.UserAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Deprecated(since = "v1.1")
public class MainSessionWrapper implements Session {
    private final MainSession mainSession = new MainSession();

    private List<RestRequestWrapper> restRequests;
    private List<DatabaseRequestWrapper> databaseRequests;
    private List<LocalRequestWrapper> localRequests;
    private List<FtpRequestWrapper> ftpRequests;
    private List<MailRequestWrapper> mailRequests;
    private List<DirectoryRequestWrapper> ldapRequests;
    private List<UserAction> userActions;

    private List<ExceptionTrace> exceptions;

    private String appName;
    private String os;
    private String re;
    private String address;


    public void setRequestsMask(int requestsMask) {
        mainSession.setRequestsMask(requestsMask);
    }

    public int getRequestsMask() {
        return mainSession.getRequestsMask();
    }

    public ExceptionTrace getException(){
        if(exceptions != null && !exceptions.isEmpty()){
            return exceptions.getLast();
        }
        return mainSession.getException();
    }

    public String getName() {
        return mainSession.getName();
    }

    public String getType() {
        return mainSession.getType();
    }

    public String getLocation() {
        return mainSession.getLocation();
    }

    public void setName(String name) {
        mainSession.setName(name);
    }

    public void setType(String type) {
        mainSession.setType(type);
    }

    public void setLocation(String location) {
        mainSession.setLocation(location);
    }

    public void setException(ExceptionTrace exception) {
        mainSession.setException(exception);
    }

    public String getUser() {
        return mainSession.getUser();
    }

    public Instant getStart() {
        return mainSession.getStart();
    }

    public Instant getEnd() {
        return mainSession.getEnd();
    }

    public String getThreadName() {
        return mainSession.getThreadName();
    }

    public UUID getSessionId() {
        return mainSession.getSessionId();
    }

    public UUID getId() {
        return mainSession.getId();
    }

    @Override
    public CompletableMetric copy() {
        throw new UnsupportedOperationException("Copying of RestSessionWrapper is not supported");
    }

    public void setUser(String user) {
        mainSession.setUser(user);
    }

    public void setStart(Instant start) {
        mainSession.setStart(start);
    }

    public void setEnd(Instant end) {
        mainSession.setEnd(end);
    }

    public void setThreadName(String threadName) {
        mainSession.setThreadName(threadName);
    }

    public void setSessionId(UUID sessionId) {
        mainSession.setSessionId(sessionId);
    }

    public void setId(UUID id) {
        mainSession.setId(id);
    }

    public void setInstanceId(UUID instanceId) {
        mainSession.setInstanceId(instanceId);
    }

    public UUID getInstanceId() {
        return mainSession.getInstanceId();
    }
}
