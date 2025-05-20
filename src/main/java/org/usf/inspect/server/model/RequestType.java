package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RequestType {
    rest("REST_REQUEST"),
    database("DATABASE_REQUEST"),
    ftp("FTP_REQUEST"),
    smtp("SMTP_REQUEST"),
    ldap("LDAP_REQUEST");

    private final String table;
}
