package org.usf.inspect.server.service;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.usf.inspect.EventTrace;
import org.usf.inspect.TracePart;
import org.usf.inspect.TraceSignal;
import org.usf.inspect.TraceUpdate;
import org.usf.inspect.server.model.Pair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor
public final class TraceCorrelator<S extends TraceSignal, U extends TraceUpdate> {

    private final Class<S> signalType;
    private final Class<U> updateType;

    private final Consumer<List<S>> signalTraceConsumer; //change to intFunction
    private final Consumer<List<U>> updateTraceConsumer; //change to intFunction
    private final Consumer<List<Pair<S, U>>> pairTraceConsumer; //change to intFunction

    public List<EventTrace> process(Collection<EventTrace> traces){
        var signals = new ArrayList<S>();
        var updates = new ArrayList<U>();
        var pairs   = new ArrayList<Pair<S, U>>();
        correlate(traces, signals, updates, pairs);
        
        var fallbackList = new ArrayList<EventTrace>();
        if(!signals.isEmpty()) {
            try {
                signalTraceConsumer.accept(signals);
            } catch (Exception e) {
                log.error("TraceCorrelator.process: error while processing {} {} traces", signals.size(), signalType.getSimpleName(), e);
                fallbackList.addAll(signals);
            }
        }
        if(!updates.isEmpty()) {
            try {
                updateTraceConsumer.accept(updates);
            } catch (Exception e) {
            	log.error("TraceCorrelator.process: error while processing {} {} traces", updates.size(), updateType.getSimpleName(), e);
                fallbackList.addAll(updates);
            }
        }
        if(!pairs.isEmpty()) {
            try {
                pairTraceConsumer.accept(pairs);
            } catch (Exception e) {
            	log.error("TraceCorrelator.process: error while processing {} pairs of [{},{}] traces", 
            			pairs.size(), signalType.getSimpleName(), updateType.getSimpleName(), e);
                pairs.forEach(ent->{
                    fallbackList.add(ent.signal());
                    fallbackList.add(ent.update());
                });
            }
        }
        return fallbackList;
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
                    	log.warn("TraceCorrelator.correlate: trace {} mismatch with pair of [{},{}]", signalType.getSimpleName(), updateType.getSimpleName());
                    }
                }
                if(sgnList.size() == 1) {
                	sgn = sgnList.get(0);
                }
                else if(sgnList.size() > 1) {
                	log.warn("TraceCorrelator.correlate", "multiple {} found for the same identifier : {}", signalType.getSimpleName(), o.getKey());
                    sgn = sgnList.stream().min(comparing(TraceSignal::getStart)).orElseThrow();
                }
                if(updList.size() == 1) {
                	upd = updList.get(0);
                }
                else if(updList.size() > 1) {
                	log.warn("TraceCorrelator.correlate: multiple {} found for the same identifier : {}", updateType.getSimpleName(), o.getKey());
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
            	log.warn("TraceCorrelator.correlate: unexpected state"); 
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

    public static <U> List<EventTrace> filterAndConsume(Collection<EventTrace> traces, BiConsumer<EventTrace, ? super Consumer<U>> traceFilter, Consumer<List<U>> filteredTraceConsumer) {
        var list = traces.stream()
                .mapMulti(traceFilter)
                .toList();
        if(!list.isEmpty()) {
            try {
                filteredTraceConsumer.accept(list);
                list = emptyList();
            } catch (Exception e) {
                log.error("TraceCorrelator.filterAndConsume: error while processing {} traces", list.size(), e);
            }
        }
        return (List<EventTrace>) list;
    }
}