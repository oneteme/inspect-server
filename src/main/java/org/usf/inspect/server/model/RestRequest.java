package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestUpdate;

import java.util.List;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class RestRequest extends AbstractRequest { //APiRequest

	private String method; //GET, POST, PUT,..
	private String protocol; //HTTP, HTTPS
	private String host; //IP, domain
	private int port; // positive number, -1 otherwise
	private String path; //request path
	private String query; //request parameters
	private String contentType; //text/html, application/json, application/xml,.. in/out ?
	private String authScheme; //Basic, Bearer, Digest, OAuth,..
	private long inDataSize; //in bytes, -1 unknown
	private long outDataSize; //in bytes, -1 unknown
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..
	private String bodyContent; //incoming content, //4xx, 5xx only
	private boolean linked;
    private List<String> intermediateNodes; //intermediate nodes
	
	@JsonCreator public RestRequest() { }
}
