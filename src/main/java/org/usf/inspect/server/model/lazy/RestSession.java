package org.usf.inspect.server.model.lazy;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RestSession extends RestRequest implements Session {
    private String name;
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

    private String userAgent;
    private String cacheControl;

    private String instanceId;
    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;
}
