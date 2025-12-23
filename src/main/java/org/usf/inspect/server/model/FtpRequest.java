package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.FtpRequestSignal;
import org.usf.inspect.core.FtpRequestUpdate;

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

    public FtpRequestSignal toRequest() {
        FtpRequestSignal ftp = new FtpRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        ftp.setUser(getUser());
        ftp.setInstanceId(getInstanceId());
        ftp.setProtocol(getProtocol());
        ftp.setHost(getHost());
        ftp.setPort(getPort());
        ftp.setServerVersion(getServerVersion());
        ftp.setClientVersion(getClientVersion());
        return ftp;
    }

    public FtpRequestUpdate toCallback() {
        FtpRequestUpdate callback = new FtpRequestUpdate(getId());
        callback.setEnd(getEnd());
        callback.setFailed(isFailed());
        callback.setCommand(getCommand());
        return callback;
    }
}
