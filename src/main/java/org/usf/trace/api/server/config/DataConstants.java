package org.usf.trace.api.server.config;

import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.OperationColumn;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

import static org.usf.jquery.core.DBComparator.*;
import static org.usf.jquery.core.DBFunction.*;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConstants {

    public static String incReqColumns(TraceApiColumn incomingRequest) {
        switch (incomingRequest) {
            case ID_INCOMING_REQ:
                return "ID_IN_REQ";
            case MTH:
                return "VA_MTH";
            case PROTOCOL:
                return "VA_PRTCL";
            case HOST:
                return "VA_HST";
            case PORT:
                return "CD_PRT";
            case PATH:
                return "VA_PTH";
            case QUERY:
                return "VA_QRY";
            case CONTENT_TYPE:
                return "VA_CNT_TYP";
            case AUTH:
                return "VA_AUTH";
            case STATUS:
                return "CD_STT";
            case SIZE_IN:
                return "VA_I_SZE";
            case SIZE_OUT:
                return "VA_O_SZE";
            case START_DATETIME:
                return "DH_DBT";
            case FINISH_DATETIME:
                return "DH_FIN";
            case THREAD:
                return "VA_THRED";
            case API_NAME:
                return "VA_API_NME";
            case USER:
                return "VA_USR";
            case APP_NAME:
                return "VA_APP_NME";
            case VERSION:
                return "VA_VRS";
            case ADDRESS:
                return "VA_ADRS";
            case ENVIRONEMENT:
                return "VA_ENV";
            case OS:
                return "VA_OS";
            case RE:
                return "VA_RE";

            default:
                throw undeclaredColumn(incomingRequest);
        }
    }

    public static DBColumn elapsedtime_Tera(TableDecorator table) {
        return c -> "(CAST (((DT_FIN - DT_DBT)  second(4)) as DECIMAL(15,2)))";
    }

    public static DBColumn asDate_Tera(TableDecorator table) {
        return c -> "(CAST(DH_DBT AS DATE))";
    }

    public static ComparisonExpression greaterOrEqualsExpressions(String timestamp) {
        return greaterOrEqual(Timestamp.from(Instant.parse(timestamp)));
    }

    public static ComparisonExpression lessThanExpressions(String timestamp) {
        return lessThan(Timestamp.from(Instant.parse(timestamp)));
    }


    public static DBColumn elapsedtime(TableDecorator table) {
        return c -> "CAST(TIMESTAMPDIFF(MILLISECOND, DH_DBT, DH_FIN) /1000.0 AS DECIMAL(10,2))";
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
        var status = STATUS.column(table);
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
        return countStatusByType(table, greaterOrEqual(200).and(lessOrEqual(226)));
    }

    private static RuntimeException undeclaredColumn(ColumnDecorator column) {
        return new IllegalArgumentException("unknown column " + column);
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
        var elapsed = elapsedtime(table);
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
