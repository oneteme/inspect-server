package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestRequest extends SessionStage {
    private String id;
    private String method;
    private String protocol;
    private String host;
    private int port;
    private String path;
    private String query;
    private String contentType;
    private String authScheme;
    private int status;
    private long inDataSize;
    private long outDataSize;
    private ExceptionInfo exception;
    private String inContentEncoding;
    private String outContentEncoding;

    private long idRequest;
    private String cdSession;
    private RestSession remoteTrace;
}
