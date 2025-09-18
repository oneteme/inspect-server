package org.usf.inspect.server.model;

import lombok.Getter;

@Getter
public enum RequestCompletableType {
    MAIN_SESSION(0),
    REST_SESSION(1),
    REST_REQUEST(2),
    JDBC_REQUEST(3),
    FTP_REQUEST(4),
    SMTP_REQUEST(5),
    LDAP_REQUEST(6),
    LOCAL_REQUEST(7);

    private final int value;

    RequestCompletableType(int value) {
        this.value = value;
    }
}
