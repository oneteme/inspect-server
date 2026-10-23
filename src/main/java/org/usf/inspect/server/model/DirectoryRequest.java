package org.usf.inspect.server.model;

import org.usf.inspect.DirectoryRequestSignal;
import org.usf.inspect.DirectoryRequestUpdate;

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
public class DirectoryRequest extends AbstractRequest {

    private String protocol;
    private String host;
    private int port;

    @JsonCreator public DirectoryRequest() { }
}
