package org.usf.trace.api.server.config;

import static org.usf.jquery.core.ComparisonExpression.equal;
import static org.usf.jquery.core.ComparisonExpression.greaterOrEqual;
import static org.usf.jquery.core.ComparisonExpression.lessThan;
import static org.usf.jquery.core.DBColumn.count;
import static org.usf.trace.api.server.config.TraceApiColumn.COMPLETE;
import static org.usf.trace.api.server.config.TraceApiColumn.END;
import static org.usf.trace.api.server.config.TraceApiColumn.START;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;

import java.sql.Timestamp;
import java.time.Instant;

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
            case ID -> "id_ses";
            case NAME -> "va_name";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case USER -> "va_usr";
            case OS -> "va_os";
            case RE -> "va_re";
            case TYPE -> "lnch";
            case LOCATION -> "loc";
            case THREAD -> "va_thred";
            case APP_NAME -> "va_app_nme";
            case VERSION -> "va_vrs";
            case ADDRESS -> "va_adrs";
            case ENVIRONEMENT -> "va_env";
            case ERR_TYPE -> "va_err_cls";
            case ERR_MSG -> "va_err_msg";
            default -> null;
        };
    }

    public static String incReqColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> "id_ses";
            case METHOD -> "va_mth";
            case PROTOCOL -> "va_prtcl";
            case HOST -> "va_hst";
            case PORT -> "cd_prt";
            case PATH -> "va_pth";
            case QUERY -> "va_qry";
            case MEDIA -> "va_cnt_typ";
            case AUTH -> "va_auth";
            case STATUS -> "cd_stt";
            case SIZE_IN -> "va_i_sze";
            case SIZE_OUT -> "va_o_sze";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case THREAD -> "va_thred";
            case API_NAME -> "va_api_nme";
            case USER -> "va_usr";
            case APP_NAME -> "va_app_nme";
            case VERSION -> "va_vrs";
            case ADDRESS -> "va_adrs";
            case ENVIRONEMENT -> "va_env";
            case OS -> "va_os";
            case RE -> "va_re";
            case ERR_TYPE -> "va_err_cls";
            case ERR_MSG -> "va_err_msg";
            default -> null;
        };
    }
    
    public static String outReqColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> "cd_api";
            case METHOD -> "va_mth";
            case PROTOCOL -> "va_prtcl";
            case HOST -> "va_hst";
            case PORT -> "cd_prt";
            case PATH -> "va_pth";
            case QUERY -> "va_qry";
            case MEDIA -> "va_cnt_typ";
            case AUTH -> "va_auth";
            case STATUS -> "cd_stt";
            case SIZE_IN -> "va_i_sze";
            case SIZE_OUT -> "va_o_sze";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case THREAD -> "va_thred";
            case ERR_TYPE -> "va_err_cls";
            case ERR_MSG -> "va_err_msg";
            case PARENT -> "cd_ses";
            default -> null;
        };
    }
    public static String outQryColumns(TraceApiColumn column) {
        return switch (column) {
            case ID -> "id_out_qry";
            case HOST -> "va_hst";
            case PORT -> "cd_prt";
            case DB -> "va_db";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case USER -> "va_usr";
            case THREAD -> "va_thred";
            case DRIVER -> "va_drv";
            case DB_NAME -> "va_db_nme";
            case DB_VERSION -> "va_db_vrs";
            case COMMANDS -> "va_cmd";
            case NAME -> "va_nme";
            case LOCATION -> "va_loc";
            case COMPLETE -> "va_cmplt";
            case PARENT -> "cd_ses";
            default -> null;
        };
    }

    public static String outStgColumns(TraceApiColumn column) {
        return switch (column) {
            case NAME -> "va_name";
            case LOCATION -> "loc";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case USER -> "va_usr";
            case THREAD -> "va_thred";
            case ERR_TYPE -> "va_err_cls";
            case ERR_MSG -> "va_err_msg";
            case PARENT -> "cd_ses";
            default -> null;
        };
    }

    public static String dbActColumns(TraceApiColumn column){
        return switch (column) {
            case TYPE -> "va_typ";
            case START -> "dh_dbt";
            case END -> "dh_fin";
            case ERR_TYPE -> "va_err_cls";
            case ERR_MSG -> "va_err_msg";
            case PARENT -> "cd_out_qry";
            case ACTION_COUNT -> "cd_count";
            default -> null;
        };
    }

    @Deprecated(forRemoval = true)
    public static ComparisonExpression greaterOrEqualsExpressions(String timestamp) {
        return greaterOrEqual(Timestamp.from(Instant.parse(timestamp)));
    }

    @Deprecated(forRemoval = true)
    public static ComparisonExpression lessThanExpressions(String timestamp) {
        return lessThan(Timestamp.from(Instant.parse(timestamp)));
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
