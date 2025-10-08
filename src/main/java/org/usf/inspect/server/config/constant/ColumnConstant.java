package org.usf.inspect.server.config.constant;

import org.usf.inspect.server.config.TraceApiColumn;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.usf.inspect.server.config.constant.FieldConstant.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColumnConstant {
    public static String mainSessionColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SES;
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case TYPE -> VA_TYP;
            case LOCATION -> VA_LCT;
            case THREAD -> VA_THR;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
            case STACKTRACE -> VA_STK;
            case MASK -> VA_MSK;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String restSessionColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SES;
            case METHOD -> VA_MTH;
            case PROTOCOL -> VA_PCL;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PATH -> VA_PTH;
            case QUERY -> VA_QRY;
            case MEDIA -> VA_CNT_TYP;
            case AUTH -> VA_ATH_SCH;
            case STATUS -> CD_STT;
            case SIZE_IN -> VA_I_SZE;
            case SIZE_OUT -> VA_O_SZE;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
            case STACKTRACE -> VA_STK;
            case CONTENT_ENCODING_IN -> VA_I_CNT_ENC;
            case CONTENT_ENCODING_OUT -> VA_O_CNT_ENC;
            case START -> DH_STR;
            case END -> DH_END;
            case THREAD -> VA_THR;
            case API_NAME -> VA_NAM;
            case USER -> VA_USR;
            case USER_AGT -> VA_USR_AGT;
            case CACHE_CONTROL -> VA_CCH_CTR;
            case MASK -> VA_MSK;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String restSessionStageColumns(TraceApiColumn column) {
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ORDER -> CD_ORD;
            case PARENT -> CD_PRN_SES;
            default -> null;
        };
    }

    public static String restRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_RST_RQT;
            case METHOD -> VA_MTH;
            case PROTOCOL -> VA_PCL;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PATH -> VA_PTH;
            case QUERY -> VA_QRY;
            case MEDIA -> VA_CNT_TYP;
            case AUTH -> VA_ATH_SCH;
            case STATUS -> CD_STT;
            case SIZE_IN -> VA_I_SZE;
            case SIZE_OUT -> VA_O_SZE;
            case BODY_CONTENT -> VA_BODY_CONTENT;
            case CONTENT_ENCODING_IN -> VA_I_CNT_ENC;
            case CONTENT_ENCODING_OUT -> VA_O_CNT_ENC;
            case START -> DH_STR;
            case END -> DH_END;
            case THREAD -> VA_THR;
            case LINKED -> VA_LNK;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String restRequestStageColumns(TraceApiColumn column) {
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ORDER -> CD_ORD;
            case PARENT -> CD_RST_RQT;
            default -> null;
        };
    }

    public static String ftpRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_FTP_RQT;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PROTOCOL -> VA_PCL;
            case SERVER_VERSION -> VA_SRV_VRS;
            case CLIENT_VERSION -> VA_CLT_VRS;
            case USER -> VA_USR;
            case START -> DH_STR;
            case END -> DH_END;
            case THREAD -> VA_THR;
            case COMMAND -> VA_CMD;
            case FAILED -> VA_FAIL;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String databaseRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_DTB_RQT;
            case SCHEME -> VA_SHE;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case DB -> VA_NAM;
            case SCHEMA -> VA_SHA;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case DRIVER -> VA_DRV;
            case DB_NAME -> VA_PRD_NAM;
            case DB_VERSION -> VA_PRD_VRS;
            case COMMAND -> VA_CMD;
            case FAILED -> VA_FAIL;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String localRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_LCL_RQT;
            case NAME -> VA_NAM;
            case TYPE -> VA_TYP;
            case LOCATION -> VA_LCT;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case FAILED -> VA_FAIL;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String ftpStageColumns(TraceApiColumn column){
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ARG -> VA_ARG;
            case COMMAND -> VA_CMD;
            case ORDER -> CD_ORD;
            case PARENT -> CD_FTP_RQT;
            default -> null;
        };
    }

    public static String databaseStageColumns(TraceApiColumn column){
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ACTION_COUNT -> VA_CNT;
            case ARG -> VA_ARG;
            case COMMAND -> VA_CMD;
            case ORDER -> CD_ORD;
            case PARENT -> CD_DTB_RQT;
            default -> null;
        };
    }

    public static String smtpRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SMTP_RQT;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case COMMAND -> VA_CMD;
            case FAILED -> VA_FAIL;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String smtpStageColumns(TraceApiColumn column){
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case COMMAND -> VA_CMD;
            case ORDER -> CD_ORD;
            case PARENT -> CD_SMTP_RQT;
            default -> null;
        };
    }

    public static String smtpMailColumns(TraceApiColumn column){
        return switch (column) {
            case SUBJECT -> VA_SBJ;
            case FROM -> VA_FRM;
            case RECIPIENTS -> VA_RCP;
            case MEDIA -> VA_CNT_TYP;
            case REPLY_TO -> VA_RPL;
            case SIZE -> VA_SZE;
            case PARENT -> CD_SMTP_RQT;
            default -> null;
        };
    }

    public static String ldapRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_LDAP_RQT;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PROTOCOL -> VA_PCL;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case COMMAND -> VA_CMD;
            case FAILED -> VA_FAIL;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String ldapStageColumns(TraceApiColumn column){
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ARG -> VA_ARG;
            case COMMAND -> VA_CMD;
            case ORDER -> CD_ORD;
            case PARENT -> CD_LDAP_RQT;
            default -> null;
        };
    }

    public static String exceptionColumns(TraceApiColumn column) {
        return switch (column) {
            case TYPE -> VA_TYP;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
            case STACKTRACE -> VA_STK;
            case ORDER -> CD_ORD;
            case PARENT -> CD_RQT;
            default -> null;
        };
    }

    public static String instanceColumns(TraceApiColumn column){
        return switch (column) {
            case ID -> ID_INS;
            case TYPE -> VA_TYP;
            case START -> DH_STR;
            case END -> DH_END;
            case APP_NAME -> VA_APP;
            case VERSION -> VA_VRS;
            case ADDRESS -> VA_ADR;
            case ENVIRONEMENT -> VA_ENV;
            case OS -> VA_OS;
            case RE -> VA_RE;
            case USER -> VA_USR;
            case COLLECTOR -> VA_CLR;
            case BRANCH -> VA_BRCH;
            case HASH -> VA_HSH;
            case CONFIGURATION -> VA_CNF;
            case RESOURCE -> VA_RSR;
            case ADDITIONAL_PROPERTIES -> VA_ADD_PRP;
            default -> null;
        };
    }

    public static String userActionColumns(TraceApiColumn column){
        return switch (column) {
            case TYPE -> VA_TYP;
            case NAME -> VA_NAM;
            case NODE_NAME -> VA_NDE_NAM;
            case START -> DH_STR;
            case PARENT -> CD_PRN_SES;
            default -> null;
        };
    }

    public static String instanceTraceColumns(TraceApiColumn column) {
        return switch (column) {
            case PENDING -> VA_PND;
            case ATTEMPTS -> VA_ATP;
            case TRACE_COUNT -> VA_TRC_CNT;
            case FILENAME -> VA_FILENAME;
            case START -> DH_STR;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String logEntryColumns(TraceApiColumn column) {
        return switch (column) {
            case LOG_LEVEL -> VA_LVL;
            case LOG_MESSAGE -> VA_MSG;
            case STACKTRACE -> VA_STK;
            case START -> DH_STR;
            case PARENT -> CD_PRN_SES;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }

    public static String resourceUsageColumns(TraceApiColumn column) {
        return switch (column) {
            case USED_HEAP -> VA_USED_HEP;
            case COMMITED_HEAP -> VA_COMMITED_HEP;
            case USED_DISK_SPACE -> VA_USED_DISK_SPACE;
            case START -> DH_STR;
            case INSTANCE_ENV -> CD_INS;
            default -> null;
        };
    }
}
