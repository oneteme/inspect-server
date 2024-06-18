package org.usf.trace.api.server.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;
import org.usf.jquery.core.RequestQueryBuilder;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;
import org.usf.trace.api.server.Constants;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.traceapi.core.InstanceType;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.sql.Timestamp.from;
import static java.util.Optional.ofNullable;
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.INSTANCE;
@Deprecated
@Repository
public class JqueryRequestService {

    private final DataSource ds;
    private List<InstanceEnvironmentWrapper> cache = new ArrayList<>();

    public JqueryRequestService(DataSource ds) {
        this.ds = ds;
    }

    //@PostConstruct
    public void instanceCache() {
        this.cache = getInstanceEnvironments();
    }

    public List<InstanceEnvironmentWrapper> cache() {
        return this.cache;
    }
    private List<InstanceEnvironmentWrapper> getInstanceEnvironments() {
        var v = new RequestQueryBuilder();
        v.select(
                INSTANCE.table(),
                getColumns(INSTANCE, ID, TYPE, START,
                        APP_NAME, VERSION, ADDRESS, ENVIRONEMENT,
                        OS, RE, COLLECTOR
                )
        );
        v.filters(INSTANCE.column(START).equal(from(Constants.DEFAULT_DATE)));
        v.orders(INSTANCE.column(TYPE).order());
        return v.build().execute(ds, rs -> {
            List<InstanceEnvironmentWrapper> instanceEnvironments = new ArrayList<>();
            while (rs.next()) {
                InstanceEnvironmentWrapper instance = new InstanceEnvironmentWrapper(
                        rs.getString(ID.reference()), rs.getString(APP_NAME.reference()), rs.getString(VERSION.reference()),
                        rs.getString(ADDRESS.reference()), rs.getString(ENVIRONEMENT.reference()),
                        rs.getString(OS.reference()), rs.getString(RE.reference()),
                        rs.getString(USER.reference()), InstanceType.valueOf(rs.getString(TYPE.reference())),
                        fromNullableTimestamp(rs.getTimestamp(START.reference())), rs.getString(COLLECTOR.reference())
                );
                instanceEnvironments.add(instance);
            }
            return instanceEnvironments;
        });
    }

    private static TaggableColumn[] getColumns(TableDecorator table, ColumnDecorator... columns) {
        return Stream.of(columns).map(table::column).toArray(TaggableColumn[]::new);
    }

    private static Instant fromNullableTimestamp(Timestamp timestamp) {
        return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
    }
}
