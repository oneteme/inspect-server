package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScriptService {
    private final JdbcTemplate template;

    private static final Set<String> tables = Set.of(
            "e_rst_rqt", "e_main_ses", "e_rst_ses",
            "e_smtp_rqt", "e_smtp_stg", "e_ftp_rqt",
            "e_ftp_stg", "e_ldap_rqt", "e_ldap_stg",
            "e_dtb_rqt", "e_dtb_stg", "e_lcl_rqt"
    );

    @Transactional(rollbackFor = Throwable.class)
    public List<String> createPartitions(YearMonth start, YearMonth end) {
        var countMonth = end.compareTo(start) + 1;
        var sdt = start.atDay(1).atStartOfDay();
        List<String> scripts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for(int i = 0; i < countMonth; i++) {
            var edt = sdt.plusMonths(1);
            for(String table : tables) {
                var name = String.format("%s_partitioned_%s_%02d", table, sdt.getYear(), sdt.getMonthValue());
                scripts.add(String.format("CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');", name, table, sdt, edt));
                names.add(name);
            }
            sdt = edt;
        }
        template.batchUpdate(scripts.toArray(String[]::new));
        return names;
    }
}
