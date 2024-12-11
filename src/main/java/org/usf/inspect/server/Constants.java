package org.usf.inspect.server;

import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.QueryBuilder;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;

import java.time.Instant;

public final class Constants {
    public static final Instant DEFAULT_DATE = Instant.parse("2023-12-31T23:00:00.000Z");

    public static final QueryBuilder test = new QueryBuilder().columns(
            REST_SESSION.column(ID),
            REST_SESSION.column(API_NAME),
            REST_SESSION.column(METHOD),
            REST_SESSION.column(PROTOCOL),
            REST_SESSION.column(HOST),
            REST_SESSION.column(PORT),
            REST_SESSION.column(PATH),
            REST_SESSION.column(QUERY),
            REST_SESSION.column(MEDIA),
            REST_SESSION.column(AUTH),
            REST_SESSION.column(STATUS),
            REST_SESSION.column(SIZE_IN),
            REST_SESSION.column(SIZE_OUT),
            REST_SESSION.column(CONTENT_ENCODING_IN),
            REST_SESSION.column(CONTENT_ENCODING_OUT),
            REST_SESSION.column(START),
            REST_SESSION.column(END),
            REST_SESSION.column(THREAD),
            REST_SESSION.column(ERR_TYPE),
            REST_SESSION.column(ERR_MSG),
            REST_SESSION.column(MASK),
            REST_SESSION.column(USER),
            REST_SESSION.column(USER_AGT),
            REST_SESSION.column(CACHE_CONTROL),
            REST_SESSION.column(INSTANCE_ENV)
    );
}
