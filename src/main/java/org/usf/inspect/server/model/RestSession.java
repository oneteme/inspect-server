package org.usf.inspect.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.HttpSession2;
import org.usf.inspect.core.HttpSessionCallback;

/**
 * 
 * @author u$f
 *
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

	@JsonCreator public RestSession() {
		this.rest = new RestRequest();
	}

    public HttpSession2 toSession() {
        HttpSession2 ses = new HttpSession2(getId(), getStart(), getThreadName());
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
        ses.setCacheControl(getCacheControl());
        ses.setException(getException());
        return ses;
    }

    public HttpSessionCallback toCallback() {
        HttpSessionCallback cb = new HttpSessionCallback(getId());
        cb.setEnd(getEnd());
        cb.setDataSize(getOutDataSize());
        cb.setContentEncoding(getOutContentEncoding());
        cb.setName(getName());
        cb.setUser(getUser());
        cb.setUserAgent(getUserAgent());
        cb.setCacheControl(getCacheControl());
        cb.setBodyContent(getBodyContent());
        cb.setStatus(getStatus());
        cb.setContentType(getContentType());
        cb.setRequestMask(getRequestsMask());
        cb.setException(getException());
        return cb;
    }
}