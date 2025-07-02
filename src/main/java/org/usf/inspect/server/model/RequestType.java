package org.usf.inspect.server.model;

import org.usf.inspect.server.config.TraceApiTable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RequestType {
    rest_session("REST_SESSION", TraceApiTable.REST_SESSION),
    main_session("MAIN_SESSION", TraceApiTable.REST_SESSION),
    rest("REST_REQUEST", TraceApiTable.REST_SESSION),
    jdbc("DATABASE_REQUEST", TraceApiTable.REST_SESSION),
    ftp("FTP_REQUEST", TraceApiTable.REST_SESSION),
    smtp("SMTP_REQUEST", TraceApiTable.REST_SESSION),
    ldap("LDAP_REQUEST", TraceApiTable.REST_SESSION);

    private final String table;
    private final TraceApiTable view;
}
