package org.usf.inspect.server.model;

import static java.util.Objects.nonNull;

import java.util.UUID;

import org.usf.inspect.core.TimedTrace;

/**
 * 
 * @author u$f
 *
 */
@Deprecated
public interface CompletableMetric extends TimedTrace {
	
	UUID getId();

	CompletableMetric copy();
	
	default boolean wasCompleted(){
		return nonNull(getEnd());
	}
}
