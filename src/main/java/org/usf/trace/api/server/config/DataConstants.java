package org.usf.trace.api.server.config;

import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.OperationColumn;
import org.usf.jquery.web.TableDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

import static org.usf.jquery.core.ComparisonExpression.*;
import static org.usf.jquery.core.DBColumn.*;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConstants {
	
    public static String sessionColumns(TraceApiColumn column) {
        switch (column) {
            case ID: 			return "ID_SES";
            case NAME: 			return "VA_NAME";
            case START: 		return "DH_DBT";
            case END: 			return "DH_FIN";
            case USER: 			return "VA_USR";
            case OS: 			return "VA_OS";
            case RE: 			return "VA_RE";
            case TYPE: 			return "LNCH";
            case LOCATION: 		return "LOC";
            case THREAD: 		return "VA_THRED";
            case APP_NAME: 		return "VA_APP_NME";
            case VERSION: 		return "VA_VRS";
            case ADDRESS: 		return "VA_ADRS";
            case ENVIRONEMENT: 	return "VA_ENV";
            case ERR_TYPE: 		return "VA_ERR_CLS";
            case ERR_MSG:		return "VA_ERR_MSG";
            default: 			return null;
        }
    }

    public static String incReqColumns(TraceApiColumn column) {
        switch (column) {
            case ID: 			return "ID_SES";
            case METHOD: 		return "VA_MTH";
            case PROTOCOL: 		return "VA_PRTCL";
            case HOST: 			return "VA_HST";
            case PORT: 			return "CD_PRT";
            case PATH: 			return "VA_PTH";
            case QUERY: 		return "VA_QRY";
            case MEDIA: 		return "VA_CNT_TYP";
            case AUTH: 			return "VA_AUTH";
            case STATUS: 		return "CD_STT";
            case SIZE_IN: 		return "VA_I_SZE";
            case SIZE_OUT: 		return "VA_O_SZE";
            case START: 		return "DH_DBT";
            case END: 			return "DH_FIN";
            case THREAD: 		return "VA_THRED";
            case API_NAME: 		return "VA_API_NME";
            case USER: 			return "VA_USR";
            case APP_NAME: 		return "VA_APP_NME";
            case VERSION: 		return "VA_VRS";
            case ADDRESS: 		return "VA_ADRS";
            case ENVIRONEMENT: 	return "VA_ENV";
            case OS: 			return "VA_OS";
            case RE: 			return "VA_RE";
            default: 			return null;
        }
    }
    
    public static String outReqColumns(TraceApiColumn column) {
        switch (column) {
            case ID: 			return "CD_API";
            case METHOD: 		return "VA_MTH";
            case PROTOCOL: 		return "VA_PRTCL";
            case HOST: 			return "VA_HST";
            case PORT: 			return "CD_PRT";
            case PATH: 			return "VA_PTH";
            case QUERY: 		return "VA_QRY";
            case MEDIA: 		return "VA_CNT_TYP";
            case AUTH: 			return "VA_AUTH";
            case STATUS: 		return "CD_STT";
            case SIZE_IN: 		return "VA_I_SZE";
            case SIZE_OUT: 		return "VA_O_SZE";
            case START: 		return "DH_DBT";
            case END: 			return "DH_FIN";
            case THREAD: 		return "VA_THRED";
            case PARENT: 		return "CD_SES";
            default: 			return null;
        }
    }
    public static String outQryColumns(TraceApiColumn column) {
        switch (column){
            case ID:           return "ID_OUT_QRY";
            case HOST:         return "VA_HST";
            case PORT:         return "CD_PRT";
            case SCHEMA:       return "VA_SCHMA";
            case START: 	   return "DH_DBT";
            case END: 		   return "DH_FIN";
            case USER: 		   return "VA_USR";
            case THREAD: 	   return "VA_THRED";
            case DRIVER:       return "VA_DRV";
            case DB_NAME:      return "VA_DB_NME";
            case DB_VERSION:   return "VA_DB_VRS";
            case COMPLETE:     return "VA_CMPLT";
            case PARENT:       return "CD_SES";
            default:           return null;
        }
    }

    public static String outStgColumns(TraceApiColumn column) {
        switch (column){
            case NAME: 			return "VA_NAME";
            case LOCATION: 		return "LOC";
            case START: 	   return "DH_DBT";
            case END: 		   return "DH_FIN";
            case USER: 		   return "VA_USR";
            case THREAD: 	   return "VA_THRED";
            case ERR_TYPE: 		return "VA_ERR_CLS";
            case ERR_MSG:		return "VA_ERR_MSG";
            case PARENT:       return "CD_SES";
            default:           return null;
        }
    }

    public static String dbActColumns(TraceApiColumn column){
        switch (column){
            case TYPE:         return"VA_TYP";
            case START: 		return "DH_DBT";
            case END: 			return "DH_FIN";
            case ERR_TYPE: 		return "VA_ERR_CLS";
            case ERR_MSG:		return "VA_ERR_MSG";
            case PARENT: 		return "CD_OUT_QRY";
            default: 			return null;
        }
    }

    public static DBColumn elapsedtime_Tera(TableDecorator table) {
        return c -> "(CAST (((DT_FIN - DT_DBT)  second(4)) as DECIMAL(15,2)))";
    }

    public static DBColumn asDate_Tera(TableDecorator table) {
        return c -> "(CAST(DH_DBT AS DATE))";
    }

    @Deprecated(forRemoval = true)
    public static ComparisonExpression greaterOrEqualsExpressions(String timestamp) {
        return greaterOrEqual(Timestamp.from(Instant.parse(timestamp)));
    }

    @Deprecated(forRemoval = true)
    public static ComparisonExpression lessThanExpressions(String timestamp) {
        return lessThan(Timestamp.from(Instant.parse(timestamp)));
    }


    public static DBColumn elapsedtime(TableDecorator table) {
        return c -> "CAST(TIMESTAMPDIFF(MILLISECOND, DH_DBT, DH_FIN) /1000.0 AS DECIMAL(10,2))";
    }

    public static DBColumn elapsedtime2(TableDecorator table) {
        return c -> "extract(EPOCH from (dh_fin - dh_dbt))";
    }

    public static DBColumn asDate(TableDecorator table) {
        return c -> "FORMATDATETIME (DH_DBT, 'yyyy-MM-dd' )";
    }

    public static DBColumn byDay(TableDecorator table) {
        return c -> "(EXTRACT (DAY FROM DH_DBT))";
    }

    public static DBColumn byMonth(TableDecorator table) {
        return c -> "(EXTRACT (MONTH FROM DH_DBT))";
    }

    public static DBColumn byYear(TableDecorator table) {
        return c -> "(EXTRACT (YEAR FROM DH_DBT))";
    }


    public static OperationColumn avgElapsedTime(TableDecorator table) {
        var elapsed = elapsedtime(table);
        return avg(elapsed);
    }

    public static OperationColumn minElapsedTime(TableDecorator table) {
        var elapsed = elapsedtime(table);
        return min(elapsed);
    }

    public static OperationColumn maxElapsedTime(TableDecorator table) {
        var elapsed = elapsedtime(table);
        return max(elapsed);
    }

    private static OperationColumn countStatusByType(TableDecorator table, ComparisonExpression op) {
        var status = table.column(STATUS);
        return count((status).when(op).then(status).end());
    }


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
        switch (name) {
            case "fastest":
                return lessThan(1);
            case "fast":
                return greaterOrEqual(1).and(lessThan(3));
            case "medium":
                return greaterOrEqual(3).and(lessThan(5));
            case "slow":
                return greaterOrEqual(5).and(lessThan(10));
            case "slowest":
                return greaterOrEqual(10);
            default:
                return null;
        }
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
