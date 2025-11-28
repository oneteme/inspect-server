package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.FtpRequest2;
import org.usf.inspect.core.FtpRequestCallback;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class FtpRequest extends AbstractRequest {

	private String protocol; //FTP, FTPS
	private String host;
	private int port;  // -1 otherwise
	private String serverVersion;
	private String clientVersion;
	private boolean failed;

	@JsonCreator() public FtpRequest() { }

    public FtpRequest2 toRequest() {
        FtpRequest2 ftp = new FtpRequest2(getId(), getSessionId(), getStart(), getThreadName());
        ftp.setUser(getUser());
        ftp.setInstanceId(getInstanceId());
        ftp.setProtocol(getProtocol());
        ftp.setHost(getHost());
        ftp.setPort(getPort());
        ftp.setServerVersion(getServerVersion());
        ftp.setClientVersion(getClientVersion());
        return ftp;
    }

    public FtpRequestCallback toCallback() {
        FtpRequestCallback callback = new FtpRequestCallback(getId());
        callback.setEnd(getEnd());
        callback.setFailed(isFailed());
        callback.setCommand(getCommand());
        return callback;
    }
}
