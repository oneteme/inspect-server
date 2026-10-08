package org.usf.inspect.server.service;

import static java.util.Objects.isNull;

import java.util.List;

import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.server.exception.DispatchProcessingException;

/**
 * 
 * @author u$f
 *
 */
public final class TraceBatchDispatcherHub extends TraceDispatcherHub {
	
	public TraceBatchDispatcherHub() {
		super("inspect-srv-publisher");
	}

	public void emitTraces(List<EventTrace> traces) throws DispatchProcessingException {
		if(isNull(traces) || traces.isEmpty()) {
			return;
		}
		if(canCollect()) {
			try {
				if(!getQueue().addAll(traces)) {
					throw new DispatchProcessingException( 
							"traces were not emitted, or partially emitted, state=%s, queue size=%s".formatted(getState(), getQueue().size()), true);
				}
			}
			catch (Exception e) { //
				throw new DispatchProcessingException(
						"failed to emit traces, state=%s, queue size=%s".formatted(getState(), getQueue().size()), e, false);
			}
			finally {
				flushIfThresholdReached();
			}
		}
		else {
			throw new DispatchProcessingException(
					"traces were not emitted, state=%s, queue size=%s".formatted(getState(), getQueue().size()), true);
		}
	}
}
