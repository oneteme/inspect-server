package org.usf.inspect.server.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestDao {

	private final JdbcTemplate template;

    public List<String> selectChildsById(String id) {
        var query = "with recursive recusive(prnt,chld) as (" +
                " select ''::varchar as prnt, ?::varchar as chld " +
                " union all " +
                " select  recusive.chld::varchar, E_RST_RQT.ID_RST_RQT::varchar " +
                " from E_RST_RQT, recusive " +
                " where recusive.chld::varchar = E_RST_RQT.CD_PRN_SES::varchar " +
                ") select distinct(chld) from recusive";
        return template.query(query, (rs, row) -> rs.getString("chld"), id).stream()
        		.filter(Objects::nonNull)
        		.toList();
    }
}

