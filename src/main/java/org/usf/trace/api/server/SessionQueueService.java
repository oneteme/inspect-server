package org.usf.trace.api.server;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.usf.traceapi.core.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SessionQueueService {

    private final RequestDao dao;
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final BlockingQueue<Session> queue = new LinkedBlockingQueue<>();
    private final ScheduledFuture<?> future;
    
    public SessionQueueService(RequestDao dao, ScheduleProperties prop) {
    	this.dao = dao;
    	this.future = executor.scheduleWithFixedDelay(this::safeBackup, 0, prop.getPeriod(), TimeUnit.valueOf(prop.getUnit()));
    }
    
    public void add(Session session) {
        queue.add(session);
        log.info("new request added to the queue : {} session(s)", queue.size());
    }
    
    public Collection<Session> waitList(){
    	return new ArrayList<>(queue); // send copy
    }

    private void safeBackup() {
    	if(!queue.isEmpty()) {
	        var list = new LinkedList<Session>();
	        log.info("scheduled data queue backup : {} session(s)", queue.drainTo(list));
	    	try {
		        dao.saveSessions(list);
	    	}
	    	catch (Exception e) {
	    		log.error("error while saving sessions", e);
	    		queue.addAll(list); // retry later
			}
    	}
    }
}
