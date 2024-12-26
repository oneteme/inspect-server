package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RestSession extends RestRequest implements Session {
    private String name;
    private List<RestRequest> restRequests;
    private List<DatabaseRequest> databaseRequests;
    private List<LocalRequest> localRequests;

    private List<FtpRequest> ftpRequests;
    private List<MailRequest> mailRequests;
    private List<NamingRequest> ldapRequests;
    private String userAgent;
    private String cacheControl;

    private String instanceId;
    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;
}
