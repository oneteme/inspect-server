package org.usf.inspect.server.model;

import static java.util.Objects.nonNull;

import java.util.Objects;

import org.usf.inspect.core.Metric;

/**
 * 
 * @author u$f
 *
 */
@Deprecated
public interface CompletableMetric extends Metric {
	
	String getId();

	CompletableMetric copy();
	
	default boolean wasCompleted(){
		return nonNull(getEnd());
	}
}
