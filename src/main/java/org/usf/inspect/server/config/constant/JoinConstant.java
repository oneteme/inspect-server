package org.usf.inspect.server.config.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.usf.inspect.server.RequestType;
import org.usf.jquery.core.ViewJoin;
import org.usf.jquery.web.JoinBuilder;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.jquery.core.ViewJoin.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JoinConstant {
    public static final String EXCEPTION_JOIN = "exception";
    public static final String INSTANCE_JOIN = "instance";
    public static final String REST_REQUEST_TO_REST_SESSION = "rest_request_to_rest_session";
    public static final String DATABASE_TO_REST_SESSION = "database_to_rest_session";
    public static final String FTP_TO_REST_SESSION = "ftp_to_rest_session";
    public static final String SMTP_TO_REST_SESSION = "smtp_to_rest_session";
    public static final String LDAP_TO_REST_SESSION = "ldap_to_rest_session";
    public static final String REST_REQUEST_JOIN = "rest_request";
    public static final String LOCAL_REQUEST_JOIN = "local_request";
    public static final String DATABASE_REQUEST_JOIN = "database_request";
    public static final String FTP_REQUEST_JOIN = "ftp_request";
    public static final String SMTP_REQUEST_JOIN = "smtp_request";
    public static final String LDAP_REQUEST_JOIN = "ldap_request";

  //  public static final String

    public static JoinBuilder mainSessionJoins(String name) {
        return switch (name) {
            case INSTANCE_JOIN ->
                    c -> new ViewJoin[]{innerJoin(INSTANCE.view(), MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder restSessionJoins(String name) {
        return switch (name) {
            case "dependencies" ->
                    c -> new ViewJoin[]{innerJoin(REST_REQUEST.view(), REST_SESSION.column(ID).eq(REST_REQUEST.column(PARENT)))};
            case INSTANCE_JOIN ->
                    c -> new ViewJoin[]{innerJoin(INSTANCE.view(), REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder restRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)))};
            case REST_REQUEST_TO_REST_SESSION ->
                    c -> new ViewJoin[]{leftJoin(REST_SESSION.view(), REST_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder localRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LOCAL.name()))};
            default -> null;
        };
    }

    public static JoinBuilder databaseRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> c -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.JDBC.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case DATABASE_TO_REST_SESSION ->
                    c -> new ViewJoin[]{leftJoin(REST_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder databaseStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), DATABASE_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), DATABASE_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.JDBC.name()))};
            default -> null;
        };
    }

    public static JoinBuilder ftpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> c -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.FTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case FTP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{leftJoin(REST_SESSION.view(), FTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder ftpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), FTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), FTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.FTP.name()))};
             default -> null;
        };
    }

    public static JoinBuilder smtpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> c -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.SMTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case SMTP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{leftJoin(REST_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder smtpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), SMTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), SMTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.SMTP.name()))};
            default -> null;
        };
    }

    public static JoinBuilder ldapRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> c -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LDAP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case LDAP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{leftJoin(REST_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static JoinBuilder ldapStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    c -> new ViewJoin[]{leftJoin(EXCEPTION.view(), LDAP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), LDAP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.LDAP.name()))};
            default -> null;
        };
    }

    public static JoinBuilder exceptionJoins(String name) {
        return switch (name) {
            case REST_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(REST_REQUEST.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.REST.name()), REST_REQUEST.column(STATUS).eq(0))};
            case LOCAL_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(LOCAL_REQUEST.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LOCAL.name()))};
            case DATABASE_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(DATABASE_REQUEST.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.JDBC.name()))};
            case FTP_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(FTP_REQUEST.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.FTP.name()))};
            case SMTP_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(SMTP_REQUEST.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.SMTP.name()))};
            case LDAP_REQUEST_JOIN ->
                    c -> new ViewJoin[]{leftJoin(LDAP_REQUEST.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LDAP.name()))};
            case REST_REQUEST_TO_REST_SESSION ->
                    c -> new ViewJoin[]{ leftJoin(REST_SESSION.view(), REST_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case DATABASE_TO_REST_SESSION ->
                    c -> new ViewJoin[]{ leftJoin(REST_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case FTP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{ leftJoin(REST_SESSION.view(), FTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case SMTP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{ leftJoin(REST_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case LDAP_TO_REST_SESSION ->
                    c -> new ViewJoin[]{ leftJoin(REST_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case INSTANCE_JOIN ->
                    c   -> new ViewJoin[]{innerJoin(INSTANCE.view(), REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            default -> null;
        };
    }
}
