package org.usf.inspect.server.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public interface Session extends Metric {

    String getId(); //UUID
    void setId(String id);

    @Deprecated(since = "v1.1", forRemoval = true)
    List<RestRequest> getRestRequests();

    @Deprecated(since = "v1.1", forRemoval = true)
    List<DatabaseRequest> getDatabaseRequests();

    @Deprecated(since = "v1.1", forRemoval = true)
    List<LocalRequest> getLocalRequests();

    @Deprecated(since = "v1.1", forRemoval = true)
    List<FtpRequest> getFtpRequests();

    @Deprecated(since = "v1.1", forRemoval = true)
    List<MailRequest> getMailRequests();

    @Deprecated(since = "v1.1", forRemoval = true)
    List<NamingRequest> getLdapRequests();

    String getInstanceId(); //UUID
    void setInstanceId(String id);

    @Deprecated(since = "v1.1", forRemoval = true)
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
