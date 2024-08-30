package org.usf.inspect.server.config.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.usf.inspect.server.RequestType;
import org.usf.inspect.server.config.TraceApiTable;
import org.usf.jquery.core.ViewJoin;
import org.usf.jquery.web.JoinBuilder;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.jquery.core.ViewJoin.innerJoin;
import static org.usf.jquery.core.ViewJoin.leftJoin;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JoinConstant {
    public final static String EXCEPTION_JOIN = "exception";

    public static JoinBuilder restSessionJoins(String name) {
        return switch (name) {
            case "j1" ->  ()-> new ViewJoin[] { innerJoin(REST_REQUEST.view(), REST_SESSION.column(ID).eq(REST_REQUEST.column(PARENT))) };
            default -> null;
        };
    }

    public static JoinBuilder restRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), REST_REQUEST.column(ID).eq(EXCEPTION.column(PARENT))) };
            default -> null;
        };
    }

    public static JoinBuilder localRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), LOCAL_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LOCAL)) };
            default -> null;
        };
    }

    public static JoinBuilder databaseRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] {
                    leftJoin(EXCEPTION.view(), DATABASE_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.JDBC)) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            default -> null;
        };
    }

    public static JoinBuilder databaseStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), DATABASE_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), DATABASE_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.JDBC)) };
            default -> null;
        };
    }

    public static JoinBuilder ftpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] {
                    leftJoin(EXCEPTION.view(), FTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.FTP)) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            default -> null;
        };
    }

    public static JoinBuilder ftpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), FTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), FTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.FTP)) };
            default -> null;
        };
    }

    public static JoinBuilder smtpRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] {
                    leftJoin(EXCEPTION.view(), SMTP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.SMTP)) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            default -> null;
        };
    }

    public static JoinBuilder smtpStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), SMTP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), SMTP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.SMTP)) };
            default -> null;
        };
    }

    public static JoinBuilder ldapRequestJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] {
                    leftJoin(EXCEPTION.view(), LDAP_REQUEST.column(ID).eq(EXCEPTION.column(PARENT)), EXCEPTION.column(TYPE).eq(RequestType.LDAP)) // Dedoublonner la requete dans le cas où ya plusieurs exception
            };
            default -> null;
        };
    }

    public static JoinBuilder ldapStageJoins(String name) {
        return switch (name) {
            case EXCEPTION_JOIN ->  ()-> new ViewJoin[] { leftJoin(EXCEPTION.view(), LDAP_STAGE.column(PARENT).eq(EXCEPTION.column(PARENT)), LDAP_STAGE.column(ORDER).eq(EXCEPTION.column(ORDER)), EXCEPTION.column(TYPE).eq(RequestType.LDAP)) };
            default -> null;
        };
    }
}
