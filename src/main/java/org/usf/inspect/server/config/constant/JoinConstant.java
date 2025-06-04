package org.usf.inspect.server.config.constant;

import static org.usf.inspect.server.RequestType.FTP;
import static org.usf.inspect.server.RequestType.JDBC;
import static org.usf.inspect.server.RequestType.LDAP;
import static org.usf.inspect.server.RequestType.LOCAL;
import static org.usf.inspect.server.RequestType.REST;
import static org.usf.inspect.server.RequestType.SMTP;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.INSTANCE_ENV;
import static org.usf.inspect.server.config.TraceApiColumn.ORDER;
import static org.usf.inspect.server.config.TraceApiColumn.PARENT;
import static org.usf.inspect.server.config.TraceApiColumn.TYPE;
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
import static org.usf.inspect.server.config.TraceApiTable.USER_ACTION;
import static org.usf.jquery.core.ViewJoin.innerJoin;
import static org.usf.jquery.core.ViewJoin.leftJoin;

import org.usf.inspect.server.RequestType;
import org.usf.jquery.core.ViewJoin;
import org.usf.jquery.web.Builder;
import org.usf.jquery.web.ViewDecorator;

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
    public static final String USER_ACTION_JOIN ="user_action";

    public static Builder<ViewDecorator, ViewJoin[]> mainSessionJoins(String name) {
        return switch (name) {
            case INSTANCE_JOIN ->
            (view, args) -> new ViewJoin[]{innerJoin(INSTANCE.view(), MAIN_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            case REST_REQUEST_JOIN ->
            (view, args)  -> new ViewJoin[]{innerJoin(REST_REQUEST.view(), MAIN_SESSION.column(ID).eq(REST_REQUEST.column(PARENT)))};
            case USER_ACTION_JOIN -> 
            (view, args)  ->  new ViewJoin[]{leftJoin(USER_ACTION.view(), MAIN_SESSION.column(ID).eq(USER_ACTION.column(PARENT)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> restSessionJoins(String name) {
        return switch (name) {
            case "dependencies" ->
                    (view, args) -> new ViewJoin[]{innerJoin(REST_REQUEST.view(), view.column(ID).eq(REST_REQUEST.column(PARENT)))};
            case DATABASE_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(DATABASE_REQUEST.view(), view.column(ID).eq(DATABASE_REQUEST.column(PARENT)))};
            case FTP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(FTP_REQUEST.view(), view.column(ID).eq(FTP_REQUEST.column(PARENT)))};
            case SMTP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(SMTP_REQUEST.view(), view.column(ID).eq(SMTP_REQUEST.column(PARENT)))};
            case LDAP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(LDAP_REQUEST.view(), view.column(ID).eq(LDAP_REQUEST.column(PARENT)))};
            
//            case "request" -> {
//            	var req = org.usf.inspect.server.model.RequestType.valueOf(args[0]).getView();
//            	return new ViewJoin[]{innerJoin(req.view(), view.column(ID).eq(req.column(PARENT)))};
//            };
                    
            case INSTANCE_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(INSTANCE.view(), REST_SESSION.column(INSTANCE_ENV).eq(INSTANCE.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> restRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> 
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.REST.name()))};
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), REST_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), REST_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> localRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LOCAL.name()))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> databaseRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> (view, args) -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(JDBC.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), DATABASE_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> databaseStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), DATABASE_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), DATABASE_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(JDBC.name()))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> ftpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> (view, args) -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(FTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), FTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), FTP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> ftpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), FTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), FTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(FTP.name()))};
             default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> smtpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> (view, args) -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(SMTP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), SMTP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> smtpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), SMTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), SMTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(SMTP.name()))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> ldapRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN -> (view, args) -> new ViewJoin[]{
                    leftJoin(EXCEPTION.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LDAP.name())) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(REST_SESSION.column(ID)))};
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(MAIN_SESSION.view(), LDAP_REQUEST.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> ldapStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(EXCEPTION.view(), LDAP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), LDAP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(LDAP.name()))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> exceptionJoins(String name) {
        return switch (name) {
            case REST_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_REQUEST.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(REST.name()))};
            case LOCAL_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(LOCAL_REQUEST.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LOCAL.name()))};
            case DATABASE_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(DATABASE_REQUEST.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(JDBC.name()))};
            case FTP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(FTP_REQUEST.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(FTP.name()))};
            case SMTP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(SMTP_REQUEST.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(SMTP.name()))};
            case LDAP_REQUEST_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(LDAP_REQUEST.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(LDAP.name()))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> instanceJoins(String name) {
        return switch (name) {
            case REST_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{leftJoin(REST_SESSION.view(), INSTANCE.column(ID).eq(REST_SESSION.column(INSTANCE_ENV)))};
            default -> null;
        };
    }

    public static Builder<ViewDecorator, ViewJoin[]> userActionJoins(String name) {
        return switch (name) {
            case MAIN_SESSION_JOIN ->
                    (view, args) -> new ViewJoin[]{innerJoin(MAIN_SESSION.view(), USER_ACTION.column(PARENT).eq(MAIN_SESSION.column(ID)))};
            default -> null;
        };
    }
}
