package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.server.QueryLoader;
import org.usf.inspect.server.model.Query;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;
    private static final Logger logger = Logger.getLogger(PurgeService.class.getName());

    @Transactional(rollbackFor = Throwable.class)
    public boolean purgeData(List<String> env, List<String> appName, Instant before, List<String> version) {

        logger.log(Level.INFO, "+ Purging old Data, parameters in entry");
        logger.log(Level.INFO, "\t- Environment: " + env.toString());
        logger.log(Level.INFO, "\t- Start: " + before);
        List<Query> queries = QueryLoader.loadQueries(env,appName,before,version);

        for(Query query: queries){
           template.update(query.getSql(),query.getParams());
        }
        return true;
    }

}
