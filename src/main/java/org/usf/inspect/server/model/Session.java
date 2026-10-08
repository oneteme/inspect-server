package org.usf.inspect.server.model;

import org.usf.inspect.server.model.wrapper.*;

import java.util.List;
import java.util.UUID;

@Deprecated(since = "v1.1")
public interface Session extends CompletableMetric {
    void setId(UUID id);

    List<RestRequestWrapper> getRestRequests();

    List<DatabaseRequestWrapper> getDatabaseRequests();

    List<LocalRequestWrapper> getLocalRequests();

    List<FtpRequestWrapper> getFtpRequests();

    List<MailRequestWrapper> getMailRequests();

    List<DirectoryRequestWrapper> getLdapRequests();

    void setInstanceId(UUID instanceId);
    UUID getInstanceId();

    int getRequestsMask();
}
