package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.jdbc.core.JdbcTemplate;
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


    /*
         environement [1..1]
         application [0..n]
         before [max]
         version [0..n]
     */
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
    private String loadSqlFile(String filePath){
        try{
            InputStream inputStream = getClass().getResourceAsStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return reader.lines().collect(Collectors.joining("\n"));
        }catch( Exception e){
            throw new RuntimeException("Failed to load SQL file "+filePath, e);
        }
    }


}
        /*var restSessionQuery = "delete from e_rst_ses where id_ses in ("+ (removedIds.containsKey("e_rst_ses") && !removedIds.get("e_rst_ses").isEmpty()? String.join(", ", "?".repeat(removedIds.get("e_rst_ses").size())) +")" : "") +")";
        Object[] params  = removedIds.values().stream().flatMap(List::stream).toArray();*/

