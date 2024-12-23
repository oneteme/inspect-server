package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type")
public interface Session {

    String getId(); //UUID
    void setId(String id);

    List<RestRequest> getRestRequests();

    List<DatabaseRequest> getDatabaseRequests();

    List<LocalRequest> getLocalRequests();

    List<FtpRequest> getFtpRequests();

    List<MailRequest> getMailRequests();

    List<NamingRequest> getLdapRequests();

    String getInstanceId(); //UUID
    void setInstanceId(String id);
}
