package org.usf.trace.api.server;

import static org.usf.trace.api.server.Utils.isEmpty;

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
	
	LOCAL(1), JDBC(2), REST(4), FTP(8), SMTP(0x10);
	
	private final int value;
	
	public static int mask(Session s) {
		var v = 0;
		if(!isEmpty(s.getStages())) {
			v |= LOCAL.value;
		}
		if(!isEmpty(s.getQueries())) {
			v |= JDBC.value;
		}
		if(!isEmpty(s.getRequests())) {
			v |= REST.value;
		}
		if(!isEmpty(s.getFtpRequests())) {
			v |= FTP.value;
		}
		if(!isEmpty(s.getMailRequests())) {
			v |= SMTP.value;
		}
		return v;
	}
}
