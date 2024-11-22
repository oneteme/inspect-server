package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.usf.inspect.core.FtpRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
public class FtpRequestWrapper extends  FtpRequest{

    private long id;
    private boolean status;

    private final String cdSession;
    @JsonIgnore
    @Delegate
    private final FtpRequest ftpRequest;

    public FtpRequestWrapper(String cdSession) {
        this.cdSession = cdSession;
        this.ftpRequest = new FtpRequest();
    }

    public FtpRequestWrapper(String cdSession, FtpRequest ftpRequest) {
        this.cdSession = cdSession;
        this.ftpRequest = ftpRequest;
    }

}
