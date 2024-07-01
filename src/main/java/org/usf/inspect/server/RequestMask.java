package org.usf.inspect.server;

import static org.usf.inspect.server.Utils.isEmpty;

import org.usf.traceapi.core.Session;

import lombok.RequiredArgsConstructor;

/**
 * 
 * Optimizes application performance by efficiently managing protocol activations.
 * This enum uses a bitmask strategy to represent and check activated protocols based on session data,
 * reducing unnecessary queries to a large database table.
 *  
 * @author u$f
 *
 */
@RequiredArgsConstructor
public enum RequestMask {
	
	LOCAL(0x1), JDBC(0x2), REST(0x4), FTP(0x8), SMTP(0x10), LDAP(0x20);
	
	private final int value;
	
	public static int mask(Session s) {
		var v = 0;
		if(!isEmpty(s.getLocalRequests())) {
			v |= LOCAL.value;
		}
		if(!isEmpty(s.getDatabaseRequests())) {
			v |= JDBC.value;
		}
		if(!isEmpty(s.getRestRequests())) {
			v |= REST.value;
		}
		if(!isEmpty(s.getFtpRequests())) {
			v |= FTP.value;
		}
		if(!isEmpty(s.getMailRequests())) {
			v |= SMTP.value;
		}
		if(!isEmpty(s.getLdapRequests())) {
			v |= LDAP.value;
		}
		return v;
	}
}
