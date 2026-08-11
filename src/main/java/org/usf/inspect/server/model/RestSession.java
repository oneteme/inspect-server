package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.HttpSessionSignal;
import org.usf.inspect.core.HttpSessionUpdate;

/**
 * Represents a traced REST session together with its request metadata and completion details.
 *
 * @author u$f
 */
@Getter
@Setter
public class RestSession extends AbstractSession {

	@JsonIgnore
	@Delegate
	private final RestRequest rest;
	private String name; //api name
	private String userAgent; //Mozilla, Chrome, curl, Postman,..
	private String cacheControl; //max-age, no-cache
	private ExceptionInfo exception;

    /**
     * Creates an empty REST session backed by a new REST request instance.
     */
	@JsonCreator public RestSession() {
		this.rest = new RestRequest();
	}

    /**
     * Converts this REST session into an outbound session signal.
     *
     * @return the session signal built from this REST session.
     */
    public HttpSessionSignal toSession() {
        HttpSessionSignal ses = new HttpSessionSignal(getId(), getStart(), getThreadName());
        ses.setMethod(getMethod());
        ses.setProtocol(getProtocol());
        ses.setHost(getHost());
        ses.setPort(getPort());
        ses.setPath(getPath());
        ses.setQuery(getQuery());
        ses.setAuthScheme(getAuthScheme());
        ses.setDataSize(getInDataSize());
        ses.setContentEncoding(getInContentEncoding());
        ses.setName(getName());
        ses.setUser(getUser());
        ses.setInstanceId(getInstanceId());
        ses.setLinked(isLinked());
        ses.setUserAgent(getUserAgent());
        return ses;
    }

    /**
     * Converts this REST session into an outbound session update callback.
     *
     * @return the session update built from this REST session.
     */
    public HttpSessionUpdate toCallback() {
        HttpSessionUpdate cb = new HttpSessionUpdate(getId());
        cb.setEnd(getEnd());
        cb.setDataSize(getOutDataSize());
        cb.setContentEncoding(getOutContentEncoding());
        cb.setName(getName());
        cb.setUser(getUser());
        cb.setCacheControl(getCacheControl());
        cb.setBodyContent(getBodyContent());
        cb.setStatus(getStatus());
        cb.setContentType(getContentType());
        cb.setRequestMask(getRequestsMask());
        cb.setException(getException());
        return cb;
    }
}