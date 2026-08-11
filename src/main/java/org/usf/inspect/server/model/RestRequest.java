package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestUpdate;

/**
 * Represents a traced REST request together with its transport and payload metadata.
 *
 * @author u$f
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
	private int status; //2xx, 4xx, 5xx, 0 otherwise
	private long inDataSize; //in bytes, -1 unknown
	private long outDataSize; //in bytes, -1 unknown
	private String inContentEncoding; //gzip, compress, identity,..
	private String outContentEncoding; //gzip, compress, identity,..
	private String bodyContent; //incoming content, //4xx, 5xx only
	private boolean linked;
	
    /**
     * Creates an empty REST request.
     */
	@JsonCreator public RestRequest() { }

    /**
     * Converts this REST request into an outbound request signal.
     *
     * @return the request signal built from this REST request.
     */
    public HttpRequestSignal toRequest() {
        HttpRequestSignal req = new HttpRequestSignal(getId(), getSessionId(), getStart(), getThreadName());
        req.setProtocol(getProtocol());
        req.setHost(getHost());
        req.setPort(getPort());
        req.setMethod(getMethod());
        req.setPath(getPath());
        req.setQuery(getQuery());
        req.setAuthScheme(getAuthScheme());
        req.setDataSize(getOutDataSize());
        req.setContentEncoding(getOutContentEncoding());
        req.setUser(getUser());
        req.setInstanceId(getInstanceId());
        return req;
    }

    /**
     * Converts this REST request into an outbound request update callback.
     *
     * @return the request update built from this REST request.
     */
    public HttpRequestUpdate toCallback() {
        HttpRequestUpdate cb = new HttpRequestUpdate(getId());
        cb.setStatus(getStatus());
        cb.setContentType(getContentType());
        cb.setDataSize(getInDataSize());
        cb.setContentEncoding(getInContentEncoding());
        cb.setBodyContent(getBodyContent());
        cb.setEnd(getEnd());
        cb.setLinked(isLinked());
        cb.setCommand(getCommand());
        return cb;
    }
}