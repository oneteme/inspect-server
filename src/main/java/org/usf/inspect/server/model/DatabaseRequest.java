package org.usf.inspect.server.model;

import org.usf.inspect.DatabaseRequestSignal;
import org.usf.inspect.DatabaseRequestUpdate;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

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

    @JsonCreator public DatabaseRequest() { }
}
