package org.usf.inspect.server.dao;

import static org.usf.inspect.server.Utils.fromNullableInstant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestDao {

	private final JdbcTemplate template;

    public Set<String> selectChildsById(String id, Instant start) {
        var query = "with recursive recusive(prnt,chld) as (" +
                " select null::uuid as prnt, ?::uuid as chld " +
                " union all " +
                " select  recusive.chld, E_RST_RQT.ID_RST_RQT " +
                " from E_RST_RQT, recusive " +
                " where recusive.chld = E_RST_RQT.CD_PRN_SES " +
                " and E_RST_RQT.DH_STR >= ? " + // constant lower bound => enables partition pruning
                ") select distinct(chld) from recusive";
        var set = template.query(query, (rs, row) -> rs.getString("chld"), id, fromNullableInstant(start)).stream()
        		.filter(Objects::nonNull)
        		.collect(Collectors.toSet());
        set.remove(id);
        return set;
    }
}

