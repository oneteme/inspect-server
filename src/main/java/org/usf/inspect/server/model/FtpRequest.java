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

    @JsonCreator() public FtpRequest() { }
}
