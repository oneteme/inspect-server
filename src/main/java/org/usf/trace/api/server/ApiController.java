package org.usf.trace.api.server;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.usf.traceapi.core.IncomingRequest;
import org.usf.traceapi.core.OutcomingRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static java.util.Collections.synchronizedCollection;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.Utils.requireSingle;

@CrossOrigin
@RestController
@RequestMapping(value = "trace", produces = APPLICATION_JSON_VALUE)
public class ApiController {
    private final RequestDao dao;
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final BlockingQueue<IncomingRequest> incomingRequestList = new LinkedBlockingQueue<>(); //FIL
    private final TraceConfigProperties traceConfigProperties;
    private final ScheduledFuture<?> future;

    private static final Logger LOGGER = Logger.getLogger(ApiController.class.getName());

    public ApiController(RequestDao dao, TraceConfigProperties traceConfigProperties) {
        this.dao = dao;
        this.traceConfigProperties = traceConfigProperties;
        this.future = this.executor.scheduleAtFixedRate(() -> {
                    if (!incomingRequestList.isEmpty()) {
                        var list = new LinkedList<IncomingRequest>();
                        LOGGER.info("inserting " + incomingRequestList.size() + " incoming requests to database");
                        incomingRequestList.drainTo(list);
                        dao.addIncomingRequest(list);
                        LOGGER.info("Queue cleared");
                    }
                },
                0, traceConfigProperties.getPeriod(), TimeUnit.valueOf(traceConfigProperties.getTimeUnit()));  // conf
    }

    @PutMapping("incoming/request")
    public ResponseEntity<Void> saveRequest(@RequestBody IncomingRequest req) {
        incomingRequestList.add(req);//201 syncthread
        LOGGER.info("added incoming request to queue. (queue size: " + incomingRequestList.size() + ")");
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("incoming/request")
    public List<IncomingRequest> getIncomingRequestByIds(@RequestParam(defaultValue = "true", name = "lazy") boolean lazy, @RequestParam(required = false, name = "id") String[] id) { // without tree
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


