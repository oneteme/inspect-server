package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DatabaseRequest2;
import org.usf.inspect.core.DatabaseRequestCallback;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class DatabaseRequest extends AbstractRequest {

	private String scheme;
	private String host;
	private int port;
	private String name;
	private String schema;
	private String driverVersion;
	private String productName;
	private String productVersion;
	private boolean failed;

	@JsonCreator public DatabaseRequest() { }

    public DatabaseRequest2 toRequest(){
        DatabaseRequest2 req = new DatabaseRequest2(getId(), getSessionId(), getStart(), getThreadName());
        req.setScheme(getScheme());
        req.setHost(getHost());
        req.setPort(getPort());
        req.setName(getName());
        req.setSchema(getSchema());
        req.setDriverVersion(getDriverVersion());
        req.setProductName(getProductName());
        req.setProductVersion(getProductVersion());
        req.setUser(getUser());
        req.setInstanceId(getInstanceId());
        return req;
    }

    public DatabaseRequestCallback toCallback(){
        DatabaseRequestCallback cb = new DatabaseRequestCallback(getId());
        cb.setFailed(isFailed());
        cb.setEnd(getEnd());
        cb.setCommand(getCommand());
        return cb;
    }
}
