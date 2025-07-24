package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.util.List;

@Getter
@Setter
public class MainSession implements Session {
    @Delegate
    @JsonIgnore
    private final LocalRequest localRequest= new LocalRequest();
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<RestRequest> restRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<DatabaseRequest> databaseRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<LocalRequest> localRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<FtpRequest> ftpRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<MailRequest> mailRequests;
    @Deprecated(since = "v1.1", forRemoval = true)
    private List<NamingRequest> ldapRequests;
    private List<UserAction> userActions;

    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;

    @Override
    public void updateCdSession() {
        Session.super.updateCdSession();
        if(getUserActions() != null) {
            for (UserAction action : getUserActions()) {
                action.setCdSession(getId());
            }
        }
    }
}
