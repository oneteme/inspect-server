package org.usf.inspect.server.config.constant;

import static org.usf.inspect.server.RequestType.*;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.DATABASE_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.DATABASE_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.EXCEPTION;
import static org.usf.inspect.server.config.TraceApiTable.FTP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.FTP_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.INSTANCE;
import static org.usf.inspect.server.config.TraceApiTable.LDAP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.LDAP_STAGE;
import static org.usf.inspect.server.config.TraceApiTable.LOCAL_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.MAIN_SESSION;
import static org.usf.inspect.server.config.TraceApiTable.REST_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.REST_SESSION;
import static org.usf.inspect.server.config.TraceApiTable.SMTP_REQUEST;
import static org.usf.inspect.server.config.TraceApiTable.SMTP_STAGE;
import static org.usf.jquery.core.ViewJoin.innerJoin;
import static org.usf.jquery.core.ViewJoin.leftJoin;

import org.usf.inspect.server.RequestType;
import org.usf.jquery.core.ViewJoin;
import org.usf.jquery.web.JoinBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JoinConstant {
    public static final String EXCEPTION_JOIN = "exception";
    public static final String INSTANCE_JOIN = "instance";
    public static final String MAIN_SESSION_JOIN ="main_session";
    public static final String REST_SESSION_JOIN = "rest_session";
    public static final String REST_REQUEST_JOIN = "rest_request";
    public static final String LOCAL_REQUEST_JOIN = "local_request";
    public static final String DATABASE_REQUEST_JOIN = "database_request";
    public static final String FTP_REQUEST_JOIN = "ftp_request";
    public static final String SMTP_REQUEST_JOIN = "smtp_request";
    public static final String LDAP_REQUEST_JOIN = "ldap_request";

    public static JoinBuilder mainSessionJoins(String name) {
        return switch (name) {
            case INSTANCE_JOIN ->
                    () -> new ViewJoin[]{innerJoin(INSTANCE.view(), MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            case REST_REQUEST_JOIN ->
                    () -> new ViewJoin[]{innerJoin(REST_REQUEST.view(), MAIN_SESSION.column(ID).eq(REST_REQUEST.column(PARENT)))};
            default -> null;
        };
    }

    public static JoinBuilder restSessionJoins(String name) {
        return switch (name) {
            case "dependencies" ->
                    () -> new ViewJoin[]{innerJoin(REST_REQUEST.view(), REST_SESSION.column(ID).eq(REST_REQUEST.column(PARENT)))};
            case DATABASE_REQUEST_JOIN ->
                    () -> new ViewJoin[]{innerJoin(DATABASE_REQUEST.view(), REST_SESSION.column(ID).eq(DATABASE_REQUEST.column(PARENT)))};
            case FTP_REQUEST_JOIN ->
                    () -> new ViewJoin[]{innerJoin(FTP_REQUEST.view(), REST_SESSION.column(ID).eq(FTP_REQUEST.column(PARENT)))};
            case SMTP_REQUEST_JOIN ->
                    () -> new ViewJoin[]{innerJoin(SMTP_REQUEST.view(), REST_SESSION.column(ID).eq(SMTP_REQUEST.column(PARENT)))};
            case LDAP_REQUEST_JOIN ->
                    () -> new ViewJoin[]{innerJoin(LDAP_REQUEST.view(), REST_SESSION.column(ID).eq(LDAP_REQUEST.column(PARENT)))};
            case INSTANCE_JOIN ->
                    () -> new ViewJoin[]{innerJoin(INSTANCE.view(), REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder restRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.REST.name()))};
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), REST_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), REST_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder localRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LOCAL.name()))};
            default -> null;
        };
    }

    public static JoinBuilder databaseRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> ()-> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(JDBC.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder databaseStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), DATABASE_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), DATABASE_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(JDBC.name()))};
            default -> null;
        };
    }

    public static JoinBuilder ftpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> ()-> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(FTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), FTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), FTP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder ftpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), FTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), FTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(FTP.name()))};
             default -> null;
        };
    }

    public static JoinBuilder smtpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> ()-> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(SMTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder smtpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), SMTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), SMTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(SMTP.name()))};
            default -> null;
        };
    }

    public static JoinBuilder ldapRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> ()-> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LDAP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder ldapStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(EXCEPTION.view(), LDAP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), LDAP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(LDAP.name()))};
            default -> null;
        };
    }

    public static JoinBuilder exceptionJoins(String name) {
        return switch (name) {
            case REST_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_REQUEST.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(REST.name()))};
            case LOCAL_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(LOCAL_REQUEST.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LOCAL.name()))};
            case DATABASE_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(DATABASE_REQUEST.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(JDBC.name()))};
            case FTP_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(FTP_REQUEST.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(FTP.name()))};
            case SMTP_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(SMTP_REQUEST.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(SMTP.name()))};
            case LDAP_REQUEST_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(LDAP_REQUEST.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LDAP.name()))};
            default -> null;
        };
    }

    public static JoinBuilder instanceJoins(String name) {
        return switch (name) {
            case REST_SESSION_JOIN ->
                    ()-> new ViewJoin[]{leftJoin(REST_SESSION.view(), INSTANCE.column(ID).eq(REST_SESSION.column(INSTANCE_ENV)))};
            default -> null;
        };
    }
}
