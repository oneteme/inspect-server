package org.usf.inspect.server.model;

import org.usf.inspect.server.config.TraceApiTable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static org.usf.inspect.server.config.TraceApiTable.*;

@RequiredArgsConstructor
@Getter
public enum RequestType {
    rest_session(REST_SESSION),
    main_session(MAIN_SESSION),
    rest(REST_REQUEST),
    jdbc(DATABASE_REQUEST),
    ftp(FTP_REQUEST),
    smtp(SMTP_REQUEST),
    ldap(LDAP_REQUEST);

    private final TraceApiTable table;
}
