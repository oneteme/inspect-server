package org.usf.inspect.server.service;

import static java.util.Objects.isNull;
import static java.util.UUID.nameUUIDFromBytes;

import java.util.List;
import java.util.UUID;

import org.usf.inspect.EventTrace;
import org.usf.inspect.core.TraceDispatcherHub;
import org.usf.inspect.server.exception.TraceDispatchProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class TraceBatchDispatcherHub extends TraceDispatcherHub {

	public static UUID INSPECT_ID = nameUUIDFromBytes("AUTO_INSPECT".getBytes());
	private static final TraceBatchDispatcherHub DEFAULT_HUB = new TraceBatchDispatcherHub();

	public void emitTraces(List<EventTrace> traces) throws TraceDispatchProcessingException {
		if(isNull(traces) || traces.isEmpty()) {
			return;
		}
		if(canCollect()) {
			try {
				if(!getQueue().addAll(traces)) {
					throw new TraceDispatchProcessingException( 
							"traces were not emitted, or partially emitted, state=%s, queue size=%s".formatted(getState(), getQueue().size()), true);
				}
			}
			catch (Exception e) { //
				throw new TraceDispatchProcessingException(
						"failed to emit traces, state=%s, queue size=%s".formatted(getState(), getQueue().size()), e, false);
			}
			finally {
				flushIfThresholdReached();
			}
		}
		else {
			throw new TraceDispatchProcessingException(
					"traces were not emitted, state=%s, queue size=%s".formatted(getState(), getQueue().size()), true);
		}
	}
	
	public static TraceBatchDispatcherHub serverHub() {
		return DEFAULT_HUB;
	}
}
