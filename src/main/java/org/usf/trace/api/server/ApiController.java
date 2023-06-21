package org.usf.trace.api.server;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.Utils.requireSingle;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
public class ApiController {
	
    private final RequestDao dao;
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final BlockingQueue<IncomingRequest> queue = new LinkedBlockingQueue<>();
    private final ScheduledFuture<?> future;

    public ApiController(RequestDao dao, TraceConfigProperties prop) {
        this.dao = dao;
        this.future = executor.scheduleWithFixedDelay(() -> {
        	if(!queue.isEmpty()) {
                var list = new LinkedList<IncomingRequest>();
                var size = queue.drainTo(list);
                log.info("inserting {} incoming requests to database", size);
                dao.addIncomingRequest(list);
                log.info("Queue cleared");
            }
        }, 0, prop.getPeriod(), TimeUnit.valueOf(prop.getTimeUnit()));  // conf
    }

    @PutMapping("incoming/request")
    public ResponseEntity<Void> saveRequest(@RequestBody IncomingRequest req) {
        queue.add(req);
        log.info("added incoming request to queue. (queue size: {})", queue.size());
        return new ResponseEntity<>(CREATED);
    }

    @GetMapping("incoming/request")
    public List<IncomingRequest> getIncomingRequestByIds(
    		@RequestParam(defaultValue = "true", name = "lazy") boolean lazy, 
    		@RequestParam(required = false, name = "id") String[] id) { // without tree
        return dao.getIncomingRequestById(lazy, id);
    }

    @GetMapping("incoming/request/{id}")
    public IncomingRequest getIncomingRequestById(@PathVariable String id) { // without tree
        return requireSingle(dao.getIncomingRequestById(true, id));
    }

    @GetMapping("incoming/request/{id}/out")
    public OutcomingRequest getOutcomingRequestById(@PathVariable String id) {
        return dao.getOutcomingRequestById(id);
    }

    @GetMapping("incoming/request/{id}/tree") //LATER
    public IncomingRequest getIncomingRequestTreeById(@PathVariable String id) {
        return requireSingle(dao.getIncomingRequestById(true, id)); //change query
    }

}


