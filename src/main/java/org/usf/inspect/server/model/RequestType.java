package org.usf.inspect.server.model;

import org.usf.inspect.server.config.TraceApiTable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.server.config.constant.FieldConstant;

import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.inspect.server.config.constant.FieldConstant.*;

@RequiredArgsConstructor
@Getter
public enum RequestType {
    rest_session(REST_SESSION, ID_SES),
    main_session(MAIN_SESSION, ID_SES),
    rest(REST_REQUEST, ID_RST_RQT),
    jdbc(DATABASE_REQUEST, ID_DTB_RQT),
    ftp(FTP_REQUEST, ID_FTP_RQT),
    smtp(SMTP_REQUEST, ID_SMTP_RQT),
    ldap(LDAP_REQUEST, ID_LDAP_RQT);

    private final TraceApiTable table;
    private final String id;
}
