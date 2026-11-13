package org.usf.inspect.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AdminDao {
	
    private final JdbcTemplate template;

    //FIXME never call !?
    public boolean existNamespace(String namespace) {
        Integer count = template.queryForObject(
                "SELECT COUNT(*) FROM e_nsp_ins WHERE va_nam = ?",
                Integer.class,
                namespace
        );
        return count > 0;
    }

    public void saveNamespace(String namespace, String token) {
        template.update("INSERT INTO e_nsp_ins(va_nam, va_enc_tkn) VALUES(?, ?)", namespace, token);
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
}
