package org.usf.inspect.server.service;

import static java.util.Objects.isNull;

import java.util.List;

import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.server.exception.DispatchProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class TraceBatchDispatcherHub extends TraceDispatcherHub {

	private static final TraceBatchDispatcherHub DEFAULT_HUB = new TraceBatchDispatcherHub();

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
	
	public static TraceBatchDispatcherHub serverHub() {
		return DEFAULT_HUB;
	}
}
