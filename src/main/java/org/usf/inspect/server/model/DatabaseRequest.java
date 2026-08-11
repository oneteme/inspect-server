package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestUpdate;

/**
 * Represents a database request and its conversion to core database request DTOs.
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

	/**
	 * Creates an empty database request.
	 */
	@JsonCreator public DatabaseRequest() { }

    /**
     * Converts this database request to its core request signal representation.
     *
     * @return the request signal built from this database request
     */
    public DatabaseRequestSignal toRequest(){
        DatabaseRequestSignal req = new DatabaseRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
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

    /**
     * Converts this database request to its callback update representation.
     *
     * @return the callback update built from this database request
     */
    public DatabaseRequestUpdate toCallback(){
        DatabaseRequestUpdate cb = new DatabaseRequestUpdate(getId());
        cb.setFailed(isFailed());
        cb.setEnd(getEnd());
        cb.setCommand(getCommand());
        return cb;
    }
}
