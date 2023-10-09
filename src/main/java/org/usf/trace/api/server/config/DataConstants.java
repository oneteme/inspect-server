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
            case ID: 			return "id_ses";
            case NAME: 			return "va_name";
            case START: 		return "dh_dbt";
            case END: 			return "dh_fin";
            case USER: 			return "va_usr";
            case OS: 			return "va_os";
            case RE: 			return "va_re";
            case TYPE: 			return "lnch";
            case LOCATION: 		return "loc";
            case THREAD: 		return "va_thred";
            case APP_NAME: 		return "va_app_nme";
            case VERSION: 		return "va_vrs";
            case ADDRESS: 		return "va_adrs";
            case ENVIRONEMENT: 	return "va_env";
            case ERR_TYPE: 		return "va_err_cls";
            case ERR_MSG:		return "va_err_msg";
            default: 			return null;
        }
    }

    public static String incReqColumns(TraceApiColumn column) {
        switch (column) {
            case ID: 			return "id_ses";
            case METHOD: 		return "va_mth";
            case PROTOCOL: 		return "va_prtcl";
            case HOST: 			return "va_hst";
            case PORT: 			return "cd_prt";
            case PATH: 			return "va_pth";
            case QUERY: 		return "va_qry";
            case MEDIA: 		return "va_cnt_typ";
            case AUTH: 			return "va_auth";
            case STATUS: 		return "cd_stt";
            case SIZE_IN: 		return "va_i_sze";
            case SIZE_OUT: 		return "va_o_sze";
            case START: 		return "dh_dbt";
            case END: 			return "dh_fin";
            case THREAD: 		return "va_thred";
            case API_NAME: 		return "va_api_nme";
            case USER: 			return "va_usr";
            case APP_NAME: 		return "va_app_nme";
            case VERSION: 		return "va_vrs";
            case ADDRESS: 		return "va_adrs";
            case ENVIRONEMENT: 	return "va_env";
            case OS: 			return "va_os";
            case RE: 			return "va_re";
            default: 			return null;
        }
    }
    
    public static String outReqColumns(TraceApiColumn column) {
        switch (column) {
            case ID: 			return "cd_api";
            case METHOD: 		return "va_mth";
            case PROTOCOL: 		return "va_prtcl";
            case HOST: 			return "va_hst";
            case PORT: 			return "cd_prt";
            case PATH: 			return "va_pth";
            case QUERY: 		return "va_qry";
            case MEDIA: 		return "va_cnt_typ";
            case AUTH: 			return "va_auth";
            case STATUS: 		return "cd_stt";
            case SIZE_IN: 		return "va_i_sze";
            case SIZE_OUT: 		return "va_o_sze";
            case START: 		return "dh_dbt";
            case END: 			return "dh_fin";
            case THREAD: 		return "va_thred";
            case PARENT: 		return "cd_ses";
            default: 			return null;
        }
    }
    public static String outQryColumns(TraceApiColumn column) {
        switch (column){
            case ID:           return "id_out_qry";
            case HOST:         return "va_hst";
            case PORT:         return "cd_prt";
            case SCHEMA:       return "va_schma";
            case START: 	   return "dh_dbt";
            case END: 		   return "dh_fin";
            case USER: 		   return "va_usr";
            case THREAD: 	   return "va_thred";
            case DRIVER:       return "va_drv";
            case DB_NAME:      return "va_db_nme";
            case DB_VERSION:   return "va_db_vrs";
            case COMPLETE:     return "va_cmplt";
            case PARENT:       return "cd_ses";
            default:           return null;
        }
    }

    public static String outStgColumns(TraceApiColumn column) {
        switch (column){
            case NAME: 			return "va_name";
            case LOCATION: 		return "loc";
            case START: 	    return "dh_dbt";
            case END: 		    return "dh_fin";
            case USER: 		    return "va_usr";
            case THREAD: 	    return "va_thred";
            case ERR_TYPE: 	    return "va_err_cls";
            case ERR_MSG:		return "va_err_msg";
            case PARENT:        return "cd_ses";
            default:            return null;
        }
    }

    public static String dbActColumns(TraceApiColumn column){
        switch (column){
            case TYPE:          return "va_typ";
            case START: 		return "dh_dbt";
            case END: 			return "dh_fin";
            case ERR_TYPE: 		return "va_err_cls";
            case ERR_MSG:		return "va_err_msg";
            case PARENT: 		return "cd_out_qry";
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
