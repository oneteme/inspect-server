package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.util.List;

@Getter
@Setter
public class RestSession implements Session {
    private String name;
    private String userAgent;
    private String cacheControl;
    @Delegate
    @JsonIgnore
    private final RestRequest restRequest= new RestRequest();
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

    private String appName;
    private String os;
    private String re;
    private String address;
    private Integer mask;
}
