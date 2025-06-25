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

    default void updateCdSession() {
        if(getRestRequests() != null) {
            for (RestRequest request : getRestRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
        if(getDatabaseRequests() != null) {
            for (DatabaseRequest request : getDatabaseRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
        if(getLocalRequests() != null) {
            for (LocalRequest request : getLocalRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
        if(getFtpRequests() != null) {
            for (FtpRequest request : getFtpRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
        if(getMailRequests() != null) {
            for (MailRequest request : getMailRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
        if(getLdapRequests() != null) {
            for (NamingRequest request : getLdapRequests()) {
                request.setCdSession(getId());
                request.setInstanceId(getInstanceId());
            }
        }
    }
}
