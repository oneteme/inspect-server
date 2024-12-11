package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.server.QueryLoader;
import org.usf.inspect.server.model.Query;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;
    private static final Logger logger = Logger.getLogger(PurgeService.class.getName());

    @Transactional
    public boolean purgeData(String env, List<String> appName, Instant before, List<String> version) {

        logger.log(Level.INFO, "+ Purging old Data, parameters in entry");
        logger.log(Level.INFO, "\t- Environment: " + env);
        logger.log(Level.INFO, "\t- Start: " + before);

        List<Query> queries = QueryLoader.loadQueries(env,appName,before,version);
        for(Query query: queries){
            template.update(query.getSql(),query.getParams());

        }

        return true; // return temp_table count(*)/table
    }

}
