package org.usf.trace.api.server.config;

import static org.usf.jquery.core.ComparisonExpression.equal;
import static org.usf.jquery.core.ComparisonExpression.greaterOrEqual;
import static org.usf.jquery.core.ComparisonExpression.lessThan;
import static org.usf.jquery.core.DBColumn.count;
import static org.usf.trace.api.server.config.DbFields.*;
import static org.usf.trace.api.server.config.TraceApiColumn.COMPLETE;
import static org.usf.trace.api.server.config.TraceApiColumn.END;
import static org.usf.trace.api.server.config.TraceApiColumn.START;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;

import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBFunction;
import org.usf.jquery.core.OperationColumn;
import org.usf.jquery.web.TableDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConstants {
	
    public static String sessionColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SES ;
            case NAME -> VA_NAME ;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case USER -> VA_USR;
            case OS -> VA_OS;
            case RE -> VA_RE;
            case TYPE -> LNCH;
            case LOCATION -> LOC;
            case THREAD -> VA_THRED;
            case APP_NAME -> VA_APP_NME;
            case VERSION -> VA_VRS;
            case ADDRESS -> VA_ADRS;
            case ENVIRONEMENT -> VA_ENV;
            case ERR_TYPE -> VA_ERR_CLS;
            case ERR_MSG -> VA_ERR_MSG;
            default -> null;
        };
    }

    public static String incReqColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_SES;
            case METHOD -> VA_MTH;
            case PROTOCOL -> VA_PRTCL;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PATH -> VA_PTH;
            case QUERY -> VA_QRY;
            case MEDIA -> VA_CNT_TYP;
            case AUTH -> VA_AUTH;
            case STATUS -> CD_STT;
            case SIZE_IN -> VA_I_SZE;
            case SIZE_OUT -> VA_O_SZE;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case THREAD -> VA_THRED;
            case API_NAME -> VA_API_NME;
            case USER -> VA_USR;
            case APP_NAME -> VA_APP_NME;
            case VERSION -> VA_VRS;
            case ADDRESS -> VA_ADRS;
            case ENVIRONEMENT -> VA_ENV;
            case OS -> VA_OS;
            case RE -> VA_RE;
            case ERR_TYPE -> VA_ERR_CLS;
            case ERR_MSG -> VA_ERR_MSG;
            default -> null;
        };
    }
    
    public static String outReqColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> CD_API;
            case METHOD -> VA_MTH;
            case PROTOCOL -> VA_PRTCL;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case PATH -> VA_PTH;
            case QUERY -> VA_QRY;
            case MEDIA -> VA_CNT_TYP;
            case AUTH -> VA_AUTH;
            case STATUS -> CD_STT;
            case SIZE_IN -> VA_I_SZE;
            case SIZE_OUT -> VA_O_SZE;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case THREAD -> VA_THRED;
            case ERR_TYPE -> VA_ERR_CLS;
            case ERR_MSG -> VA_ERR_MSG;
            case PARENT -> CD_SES;
            default -> null;
        };
    }
    public static String outQryColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> ID_OUT_QRY;
            case HOST -> VA_HST;
            case PORT -> CD_PRT;
            case DB -> VA_DB;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case USER -> VA_USR;
            case THREAD -> VA_THRED;
            case DRIVER -> VA_DRV;
            case DB_NAME -> VA_DB_NME;
            case DB_VERSION -> VA_DB_VRS;
            case COMMANDS -> VA_CMD;
            case NAME -> VA_NME;
            case LOCATION -> VA_LOC;
            case COMPLETE -> VA_CMPLT;
            case PARENT -> CD_SES;
            default -> null;
        };
    }

    public static String outStgColumns(TraceApiColumn column) {
        return switch (column) {
            case NAME -> VA_NAME;
            case LOCATION -> LOC;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case USER -> VA_USR;
            case THREAD -> VA_THRED;
            case ERR_TYPE -> VA_ERR_CLS;
            case ERR_MSG -> VA_ERR_MSG;
            case PARENT -> CD_SES;
            default -> null;
        };
    }

    public static String dbActColumns(TraceApiColumn column){
        return switch (column) {
            case TYPE -> VA_TYP;
            case START -> DH_DBT;
            case END -> DH_FIN;
            case ERR_TYPE -> VA_ERR_CLS;
            case ERR_MSG -> VA_ERR_MSG;
            case PARENT -> CD_OUT_QRY;
            case ACTION_COUNT -> CD_COUNT;
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
