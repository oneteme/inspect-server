package org.usf.inspect.server.model.wrapper;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.MainSession;
import org.usf.inspect.server.model.Session;
import org.usf.inspect.server.model.UserAction;
import org.usf.inspect.server.model.Wrapper;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class MainSessionWrapper implements Wrapper<MainSession>, Session {
    private final MainSession mainSession = new MainSession();

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
    private List<UserAction> userActions;

    private List<ExceptionInfo> exceptions;

    private String instanceId;
    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;


    public void setRequestsMask(int requestsMask) {
        mainSession.setRequestsMask(requestsMask);
    }

    public int getRequestsMask() {
        return mainSession.getRequestsMask();
    }

    public ExceptionInfo getException(){
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

    public void setException(ExceptionInfo exception) {
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

    public String getSessionId() {
        return mainSession.getSessionId();
    }

    public String getId() {
        return mainSession.getId();
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

    public void setSessionId(String sessionId) {
        mainSession.setSessionId(sessionId);
    }

    public void setId(String id) {
        mainSession.setId(id);
    }

    @Override
    public MainSession copy() {
        return mainSession.copy();
    }

    @Override
    public boolean wasCompleted() {
        return mainSession.wasCompleted();
    }

    @Override
    public MainSession unwrap() {
        return mainSession;
    }
}
