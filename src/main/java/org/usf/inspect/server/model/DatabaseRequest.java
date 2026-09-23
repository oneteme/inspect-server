package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestUpdate;

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
