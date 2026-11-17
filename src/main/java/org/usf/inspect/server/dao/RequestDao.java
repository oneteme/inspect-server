package org.usf.inspect.server.dao;

import static java.util.stream.Collectors.toSet;
import static org.usf.inspect.server.Utils.fromNullableInstant;

import java.time.Instant;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestDao {

	private final JdbcTemplate template;

    public Set<UUID> selectChildsById(UUID id, Instant start) {
        var query = "with recursive recusive(prnt,chld) as (" +
                " select null::uuid as prnt, ? as chld " + //TODO Check h2 cast
                " union all " +
                " select  recusive.chld, E_RST_RQT.ID_RST_RQT " +
                " from E_RST_RQT, recusive " +
                " where recusive.chld = E_RST_RQT.CD_PRN_SES " +
                " and E_RST_RQT.DH_STR >= ? " + // constant lower bound => enables partition pruning
                ") select distinct(chld) from recusive";
        var set = template.query(query, (rs, row) -> rs.getObject("chld", UUID.class), id, 
        		fromNullableInstant(start)).stream()
        		.filter(Objects::nonNull)
        		.collect(toSet());
        set.remove(id);
        return set;
    }

    public List<Map<String, Object>> getDatabaseSize() {
        return template.queryForList("SELECT" +
                " n.nspname AS schema," +
                " pg_size_pretty(SUM(pg_total_relation_size(c.oid))) AS size_schema," +
                " pg_size_pretty(pg_database_size(current_database())) AS size_database" +
                " FROM pg_class c" +
                " JOIN pg_namespace n ON n.oid = c.relnamespace" +
                " WHERE c.relkind IN ('r', 'm', 'p')" +
                " AND n.nspname = current_schema()" +
                " GROUP BY n.nspname;");
    }

    public List<Map<String, Object>> getTablesSize() {
        return template.queryForList("""
                WITH RECURSIVE relations AS (
                    SELECT
                        c.oid AS root_oid,
                        c.oid AS rel_oid
                    FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = current_schema()
                      AND c.relkind IN ('r', 'p', 'm')
                      AND c.oid NOT IN (SELECT inhrelid FROM pg_inherits)
                      AND has_table_privilege(c.oid, 'SELECT')

                    UNION ALL

                    -- Add child tables of partitioned tables
                    SELECT
                        r.root_oid,
                        i.inhrelid AS rel_oid
                    FROM relations r
                    JOIN pg_inherits i ON i.inhparent = r.rel_oid
                    JOIN pg_class c ON c.oid = i.inhrelid
                    WHERE has_table_privilege(c.oid, 'SELECT')
                )
                SELECT
                    n.nspname AS schema,                    
                    c.relname AS table_name,
                    pg_size_pretty(SUM(pg_table_size(r.rel_oid))) AS table_size,
                    pg_size_pretty(SUM(pg_indexes_size(r.rel_oid))) AS indexes_size,
                    pg_size_pretty(SUM(pg_total_relation_size(r.rel_oid))) AS total_size
                FROM relations r
                    JOIN pg_class c ON c.oid = r.root_oid
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                GROUP BY c.oid, c.relname, n.nspname
                ORDER BY SUM(pg_total_relation_size(r.rel_oid)) DESC;
                """);
    }

}

