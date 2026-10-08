package org.usf.inspect.server.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class AdminDao {
    private final JdbcTemplate template;

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
}
