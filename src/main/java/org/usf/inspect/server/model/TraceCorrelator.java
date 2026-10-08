package org.usf.inspect.server.model;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.usf.inspect.server.service.TraceBatchDispatcherHub.serverHub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.TracePart;
import org.usf.inspect.core.TraceSignal;
import org.usf.inspect.core.TraceUpdate;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class TraceCorrelator<S extends TraceSignal, U extends TraceUpdate> {

    private final Class<S> signalType;
    private final Class<U> updateType;

    private final Consumer<List<S>> signalTraceConsumer;
    private final Consumer<List<U>> updateTraceConsumer;
    private final Consumer<List<Pair<S, U>>> pairTraceConsumer;

    public List<EventTrace> process(Collection<EventTrace> traces){
        var signals = new ArrayList<S>();
        var updates = new ArrayList<U>();
        var pairs   = new ArrayList<Pair<S, U>>();
        correlate(traces, signals, updates, pairs);
        var fallback = new ArrayList<EventTrace>();
        if(!signals.isEmpty()) {
            try {
                signalTraceConsumer.accept(signals);
            } catch (Exception e) {
                serverHub().emitReport("TraceCorrelator.process", 
                		"error while processing %s traces".formatted(signalType.getSimpleName()), e);
                fallback.addAll(signals);
            }
        }
        if(!updates.isEmpty()) {
            try {
                updateTraceConsumer.accept(updates);
            } catch (Exception e) {
                serverHub().emitReport("TraceCorrelator.process", 
                		"error while processing %s traces".formatted(updateType.getSimpleName()), e);
                fallback.addAll(updates);
            }
        }
        if(!pairs.isEmpty()) {
            try {
                pairTraceConsumer.accept(pairs);
            } catch (Exception e) {
                serverHub().emitReport("TraceCorrelator.process", 
                		"error while processing pairs{%s,%s} traces".formatted(signalType.getSimpleName(), updateType.getSimpleName()), e);
                pairs.forEach(ent->{
                    fallback.add(ent.signal());
                    fallback.add(ent.update());
                });
            }
        }
        return fallback;
    }
    
    void correlate(Collection<EventTrace> traces, List<S> signals, List<U> updates, List<Pair<S,U>> pairs) {
        var traceMap = traces.stream().<TracePart>mapMulti((t,cons)-> {
        	if(signalType.isInstance(t) || updateType.isInstance(t)) {
        		cons.accept((TracePart)t);
        	}
        }).collect(groupingBy(TracePart::getId));
    	for(var o : traceMap.entrySet()){
        	S sgn = null;
        	U upd = null;
        	if(o.getValue().size() == 1) {
        		var prt = o.getValue().get(0);
        		if(signalType.isInstance(prt)){
        			sgn = signalType.cast(prt);
        		}
        		else {
        			upd = updateType.cast(prt);
        		}
        	}
        	else {
                var sgnList = new ArrayList<S>();
                var updList = new ArrayList<U>();
                for (var prt : o.getValue()) {
                    if (signalType.isInstance(prt)) {
                        sgnList.add(signalType.cast(prt));
                    } else if (updateType.isInstance(prt)) {
                        updList.add(updateType.cast(prt));
                    }
                    else {
                    	serverHub().emitReport("TraceCorrelator.correlate", 
                    			"trace {} mismatch with pair{%s,%s}".formatted(signalType.getSimpleName(), updateType.getSimpleName()));
                    }
                }
                if(sgnList.size() == 1) {
                	sgn = sgnList.get(0);
                }
                else if(sgnList.size() > 1) {
                    serverHub().emitReport("TraceCorrelator.correlate", "multiple %s found for the same identifier : %s".formatted(signalType.getSimpleName(), o.getKey()));
                    sgn = sgnList.stream().min(comparing(TraceSignal::getStart)).orElseThrow();
                }
                if(updList.size() == 1) {
                	upd = updList.get(0);
                }
                else if(updList.size() > 1) {
                	serverHub().emitReport("TraceCorrelator.correlate", "multiple %s found for the same identifier : %s".formatted(updateType.getSimpleName(), o.getKey()));
                    upd = updList.stream().max(comparing(TraceUpdate::getEnd)).orElseThrow();
                }
        	}
            if(nonNull(sgn) && nonNull(upd)) {
                pairs.add(new Pair<>(sgn, upd));
            } else if(nonNull(sgn)) {
            	signals.add(sgn);
            } else if(nonNull(upd)) {
                updates.add(upd);
            }
            else {
            	serverHub().emitReport("TraceCorrelator.correlate", "unexpected state"); 
            }
        }
    }

    public static <T extends TraceSignal, U extends TraceUpdate> List<EventTrace> filterAndConsume(Collection<EventTrace> traces, Class<T> signalType, Class<U> updateType, Consumer<List<T>> updateTraceConsumer, Consumer<List<U>> updateBatchExecutor, Consumer<List<Pair<T, U>>> pairTraceConsumer) {
        return new TraceCorrelator<>(signalType, updateType, updateTraceConsumer, updateBatchExecutor, pairTraceConsumer).process(traces);
    }

    public static <U> List<EventTrace> filterAndConsume(Collection<EventTrace> traces, Class<U> traceType, Consumer<List<U>> traceTypeConsumer) {
        return filterAndConsume(traces, (e, acc) -> {
            if(traceType.isInstance(e)) {
                acc.accept(traceType.cast(e));
            }
        }, traceTypeConsumer);
    }

    public static <U> List<EventTrace> filterAndConsume(Collection<EventTrace> trace, BiConsumer<EventTrace, ? super Consumer<U>> traceFilter, Consumer<List<U>> filteredTraceConsumer) {
        var list = trace.stream()
                .mapMulti(traceFilter)
                .toList();
        if(!list.isEmpty()) {
            try {
                filteredTraceConsumer.accept(list);
                list = emptyList();
            } catch (Exception e) {
                serverHub().emitReport("TraceCorrelator.filterAndConsume", "error while processing traces", e);
            }
        }
        return (List<EventTrace>) list;
    }
}