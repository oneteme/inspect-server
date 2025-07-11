package org.usf.inspect.server.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainSession extends LocalRequest implements Session {
    private String id;
    private String type;
    private List<RestRequest> restRequests;
    private List<DatabaseRequest> databaseRequests;
    private List<LocalRequest> localRequests;

    private List<FtpRequest> ftpRequests;
    private List<MailRequest> mailRequests;
    private List<NamingRequest> ldapRequests;
    private List<UserAction> userActions;

    private String instanceId;
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
