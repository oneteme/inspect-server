package org.usf.inspect.server.config;

import static org.usf.inspect.server.config.DbFields.*;
import static org.usf.inspect.server.config.TraceApiColumn.COMPLETE;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.STATUS;
import static org.usf.jquery.core.ComparisonExpression.equal;
import static org.usf.jquery.core.ComparisonExpression.greaterOrEqual;
import static org.usf.jquery.core.ComparisonExpression.isNotNull;
import static org.usf.jquery.core.ComparisonExpression.lessThan;
import static org.usf.jquery.core.DBColumn.count;

import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBFunction;
import org.usf.jquery.core.OperationColumn;
import org.usf.jquery.web.TableDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConstants {
	
    public static String mainSessionColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SES ;
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case TYPE -> VA_TYP;
            case LOCATION -> VA_LCT;
            case THREAD -> VA_THR;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
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
            case CONTENT_ENCODING_IN -> VA_I_CNT_ENC;
            case CONTENT_ENCODING_OUT -> VA_O_CNT_ENC;
            case START -> DH_STR;
            case END -> DH_END;
            case THREAD -> VA_THR;
            case API_NAME -> VA_NAM;
            case USER -> VA_USR;
            case USER_AGT -> VA_USR_AGT;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
            case CACHE_CONTROL -> VA_CCH_CTR;
            case MASK -> VA_MSK;
            case INSTANCE_ENV -> CD_INS;
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
            case CONTENT_ENCODING_IN -> VA_I_CNT_ENC;
            case CONTENT_ENCODING_OUT -> VA_O_CNT_ENC;
            case START -> DH_STR;
            case END -> DH_END;
            case THREAD -> VA_THR;
            case PARENT -> CD_PRN_SES;
            case REMOTE -> CD_RMT_SES;
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
            case PARENT -> CD_PRN_SES;
            default -> null;
        };
    }

    public static String databaseRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_DTB_RQT;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case DB -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case DRIVER -> VA_DRV;
            case DB_NAME -> VA_PRD_NAM;
            case DB_VERSION -> VA_PRD_VRS;
            case COMMANDS -> VA_CMD;
            case COMPLETE -> VA_CPT;
            case PARENT -> CD_PRN_SES;
            default -> null;
        };
    }

    public static String localRequestColumns(TraceApiColumn column) {
        return switch (column) {
            case NAME -> VA_NAM;
            case LOCATION -> VA_LCT;
            case START -> DH_STR;
            case END -> DH_END;
            case USER -> VA_USR;
            case THREAD -> VA_THR;
            case PARENT -> CD_PRN_SES;
            default -> null;
        };
    }

    public static String ftpStageColumns(TraceApiColumn column){
        return switch (column) {
            case NAME -> VA_NAM;
            case START -> DH_STR;
            case END -> DH_END;
            case ARG -> VA_ARG;
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
            case ORDER -> CD_ORD;
            case PARENT -> CD_DTB_RQT;
            default -> null;
        };
    }

    public static String exceptionColumns(TraceApiColumn column) {
        return switch (column) {
            case TYPE -> VA_TYP;
            case ERR_TYPE -> VA_ERR_TYP;
            case ERR_MSG -> VA_ERR_MSG;
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
            case APP_NAME -> VA_APP;
            case VERSION -> VA_VRS;
            case ADDRESS -> VA_ADR;
            case ENVIRONEMENT -> VA_ENV;
            case OS -> VA_OS;
            case RE -> VA_RE;
            case USER -> VA_USR;
            case COLLECTOR -> VA_CLR;
            default -> null;
        };
    }

    public static DBColumn elapsedtime2(TableDecorator table) {
        return DBFunction.epoch().args(table.column(END).minus(table.column(START)));

    }

    private static OperationColumn countStatusByType(TableDecorator table, ComparisonExpression op) {
        var status = table.column(STATUS);
        return count((status).when(op).then(status).end());
    }
    public static DBColumn err(TableDecorator table){ // temporary solution to be changed
        return DBFunction.coalesce().args(table.column(ERR_MSG),table.column(ERR_TYPE));
    }

    public static ComparisonExpression errComp(String name){// temporary solution to be changed
        return isNotNull();
    }

    private static OperationColumn countDbBySucces(TableDecorator table, ComparisonExpression op ){
        var complete = table.column(COMPLETE);
        return count((complete).when(op).then(complete).end());
    }

    public static OperationColumn countDbError(TableDecorator table){ return countDbBySucces(table,equal('F'));}
    public static OperationColumn countDbSucces (TableDecorator table){ return countDbBySucces(table,equal('T'));}


    public static OperationColumn countStatus200(TableDecorator table) {
        return countStatusByType(table, equal(200));
    }

    public static OperationColumn countStatus400(TableDecorator table) {
        return countStatusByType(table, equal(400));
    }

    public static OperationColumn countStatus401(TableDecorator table) {
        return countStatusByType(table, equal(401));
    }

    public static OperationColumn countStatus403(TableDecorator table) {
        return countStatusByType(table, equal(403));
    }

    public static OperationColumn countStatus404(TableDecorator table) {
        return countStatusByType(table, equal(404));
    }

    public static OperationColumn countStatus500(TableDecorator table) {
        return countStatusByType(table, equal(500));
    }
    
    public static OperationColumn countStatus503(TableDecorator table) {
        return countStatusByType(table, equal(503));
    }

    public static OperationColumn countErrorStatus(TableDecorator table) {
        return countStatusByType(table, greaterOrEqual(400));
    }

    public static OperationColumn countClientErrorStatus(TableDecorator table) {
        return countStatusByType(table, greaterOrEqual(400).and(lessThan(500)));
    }

    public static OperationColumn countServerErrorStatus(TableDecorator table) {
        return countStatusByType(table, greaterOrEqual(500));
    }

    public static OperationColumn countSuccesStatus(TableDecorator table) {
        return countStatusByType(table, greaterOrEqual(200).and(lessThan(300)));
    }

    public static ComparisonExpression elapsedTimeExpressions(String name) {
        return switch (name) {
            case "fastest" -> lessThan(1);
            case "fast" -> greaterOrEqual(1).and(lessThan(3));
            case "medium" -> greaterOrEqual(3).and(lessThan(5));
            case "slow" -> greaterOrEqual(5).and(lessThan(10));
            case "slowest" -> greaterOrEqual(10);
            default -> null;
        };
    }


    private static OperationColumn elapsedTimeBySpeed(ComparisonExpression op, TableDecorator table) {
        var elapsed = elapsedtime2(table);
        return count(elapsed.when(op).then(elapsed).end());
    }

    public static OperationColumn elapsedTimeVerySlow(TableDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions("slowest"), table);
    }

    public static OperationColumn elapsedTimeSlow(TableDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions("slow"), table);
    }

    public static OperationColumn elapsedTimeMedium(TableDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions("medium"), table);
    }

    public static OperationColumn elapsedTimeFast(TableDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions("fast"), table);
    }

    public static OperationColumn elapsedTimeFastest(TableDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions("fastest"), table);
    }
}
