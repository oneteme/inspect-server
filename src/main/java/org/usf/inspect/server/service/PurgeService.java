package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;



    /*
         environement [1..1]
         application [0..n]
         before [max]
         version [0..n]
     */
    public void purgeData(String env, List<String> appName, Instant before, List<String> version ){
        var query = "With deleted_instances AS (";


    }

}
