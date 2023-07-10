package org.usf.trace.api.server;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PreDestroy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.usf.traceapi.core.Session;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableConfigurationProperties(ScheduleProperties.class)
public class SessionQueueService {

    private final RequestDao dao;
	private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final BlockingQueue<Session> queue = new LinkedBlockingQueue<>();
    
    public SessionQueueService(RequestDao dao, ScheduleProperties prop) {
    	this.dao = dao;
    	executor.scheduleWithFixedDelay(this::safeBackup, 
    			prop.getDelay()*2, prop.getDelay(), prop.getUnit()); //x2 wait for previous POD backup
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
    
    @PreDestroy
    void destroy() throws InterruptedException {
		log.info("backup before shutdown");
    	try {
    		executor.shutdown(); //cancel future
    		while(!executor.awaitTermination(5, SECONDS)); //wait for last save complete
    	}
    	finally {
    		safeBackup(); //earlier shutdown
		}
	}
    
}
