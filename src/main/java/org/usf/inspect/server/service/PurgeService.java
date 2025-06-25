package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.server.QueryLoader;
import org.usf.inspect.server.model.Query;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;

    @Transactional(rollbackFor = Throwable.class)
    public boolean purgeData(List<String> env, List<String> appName, Instant before, List<String> version) {

        log.info("+ Purging old Data, parameters in entry"); // change to inline
        log.info("\t- Environment: " + env.toString());
        log.info("\t- Start: " + before);
        List<Query> queries = QueryLoader.loadQueries(env,appName,before,version);
        Map<String, Integer> suppInfo = new HashMap<>();

        int i;
        for(Query query: queries){
            if(query.getName()!= null && !query.getName().isEmpty()){
                if(!suppInfo.containsKey(query.getName())) {
                    suppInfo.put(query.getName(), 0);
                }
                i=  template.update(query.getSql(),query.getParams()); // recup count
                suppInfo.put(query.getName(),suppInfo.get(query.getName()) + i);
            }else
                template.update(query.getSql(), query.getParams()); // recup count
        }
        log.info("+ Purge info:");
        suppInfo.forEach(((name,count) -> log.info("\t- "+name+": "+count)));
        return true;
    }

    public void vaccum(){
        String[] vacuumQueries = {
                "VACUUM ANALYZE e_dtb_rqt;",
                "VACUUM ANALYZE e_dtb_stg;",
                "VACUUM ANALYZE e_ftp_rqt;",
                "VACUUM ANALYZE e_ftp_stg;",
                "VACUUM ANALYZE e_smtp_rqt;",
                "VACUUM ANALYZE e_smtp_stg;",
                "VACUUM ANALYZE e_ldap_rqt;",
                "VACUUM ANALYZE e_ldap_stg;",
                "VACUUM ANALYZE e_lcl_rqt;",
                "VACUUM ANALYZE e_rst_ses;",
                "VACUUM ANALYZE e_rst_rqt;",
                "VACUUM ANALYZE e_main_ses;"
        };
        try {
            log.info("+ Vaccum Data");
            for (String query : vacuumQueries) {
                template.update(query);
            }
        } catch (Exception e) {
            log.error("Error while vaccuming tables", e);
        }
    }


}
