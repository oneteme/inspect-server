package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.QueryBuilder;
import org.usf.jquery.core.ViewJoin;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.jquery.core.ViewJoin.innerJoin;
import static org.usf.jquery.core.ViewJoin.leftJoin;

@Service
@RequiredArgsConstructor
public class InstanceService {
    private final DataSource ds;

    public Collection<String> getApiNames(String env, String appName) throws SQLException {
        var v = new QueryBuilder()
                .columns(REST_SESSION.column(API_NAME))
                .joins(innerJoin(REST_SESSION.view(), INSTANCE.column(ID).eq(REST_SESSION.column(INSTANCE_ENV)), INSTANCE.column(ENVIRONEMENT).eq(env), INSTANCE.column(APP_NAME).eq(appName), INSTANCE.column(TYPE).eq("SERVER")))
                .filters(REST_SESSION.column(API_NAME).notNull())
                .distinct()
                .orders();
        return v.build().execute(ds, rs -> {
            List<String> apiNames = new ArrayList<>();
            while (rs.next()) {
                apiNames.add(rs.getString(1));
            }
            return apiNames;
        });
    }
}
