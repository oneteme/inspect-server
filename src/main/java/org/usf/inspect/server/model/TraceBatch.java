package org.usf.inspect.server.model;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.usf.inspect.core.AbstractStage;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.TraceSignal;
import org.usf.inspect.core.TraceUpdate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 
 * @author u$f
 *
 */
@Getter
@ToString
@AllArgsConstructor
public final class TraceBatch implements EventTrace {

    private final Instant instant;
    private final Integer attempts;
    private final UUID instanceId;
    private final int traceCount;
    private final int pending;
    //v1.2
    private final int sequence;

    public static TraceBatch newTraceBatch(Instant instant, int sequence, int attempts, UUID instanceId, List<EventTrace> traces) {
		int pnd = 0;
		int nbr = 0;
		if(nonNull(traces) && !traces.isEmpty()) {
			for(var trc : traces) {
				if(trc instanceof TraceSignal) {
					++pnd;
				}
				else if(trc instanceof TraceUpdate) {
					--pnd;
					++nbr;
				}
				else if(trc instanceof AbstractStage) {
					++nbr;
				}
			}
		}
		return new TraceBatch(instant, attempts, instanceId, nbr, pnd, sequence);
	}
}
