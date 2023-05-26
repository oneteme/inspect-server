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
import static org.usf.trace.api.server.config.TraceApiColumn.ACTION;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataConstants {
	
    public static String agrColumns(TraceApiColumn agreementsApiColumn) {
        switch(agreementsApiColumn) {
            case STATUS: 		return "CD_STT";
            case MTH: 		return "VA_MTH";
            case URI: 			return "VA_URI";
            case START_DATETIME: 		return "DT_DBT";
            case FINISH_DATETIME: 			return "DT_FIN";
            case RESOURCE:     return "VA_RCS";
            case CLIENT: 		return "VA_CLN";
            case ACTION: 		return "VA_ACT";
            case DMN: 		return "VA_DMN";
            case API: 			return "VA_APP";
            default : throw undeclaredColumn(agreementsApiColumn);
        }
    } 
    
    public static String agrColumns1(TraceApiColumn agreementsApiColumn) {
        switch(agreementsApiColumn) {
            case STATUS: 		return "CD_STT";
            case MTH: 		return "VA_MTH";
            case URI: 			return "VA_URI";
            case START_DATETIME: 		return "DT_DBT";
            case FINISH_DATETIME: 			return "DT_FIN";
            case RESOURCE:     return "VA_RCS";
            case CLIENT: 		return "VA_CLN";
            case ACTION: 		return "VA_ACT";
            case DMN: 		return "VA_DMN";
            case API: 			return "VA_APP";
            default : throw undeclaredColumn(agreementsApiColumn);
        }
    }

    public static ComparisonExpression greaterOrEqualsExpressions(String timestamp) {
        return greaterOrEqual(Timestamp.from(Instant.parse(timestamp)));
    }
    public static ComparisonExpression lessThanExpressions(String timestamp) {
        return lessThan(Timestamp.from(Instant.parse(timestamp)));
    }

    public static DBColumn elapsedtime(TableDecorator table){
        return c -> "(CAST (((DT_FIN - DT_DBT)  second(4)) as DECIMAL(15,2)))";
    }

    public static DBColumn asDate(TableDecorator table){
        return c->"(CAST(DT_DBT AS DATE))";
    }

    public static DBColumn byDay(TableDecorator table){
        return c->"(EXTRACT (DAY FROM DT_DBT))";
    }
    public static DBColumn byMonth(TableDecorator table){
        return c->"(EXTRACT (MONTH FROM DT_DBT))";
    }
    public static DBColumn byYear(TableDecorator table){
        return c->"(EXTRACT (YEAR FROM DT_DBT))";
    }


    public static final OperationColumn  avgElapsedTime (TableDecorator  table){
        var elapsed = elapsedtime(table);
        return avg(elapsed);
    }
    public static final OperationColumn minElapsedTime(TableDecorator  table){
        var elapsed = elapsedtime(table);
        return min(elapsed);
    }

    public static final OperationColumn maxElapsedTime(TableDecorator  table){
        var elapsed = elapsedtime(table);
        return max(elapsed);
    }
    public static final OperationColumn getActionPerimeter(TableDecorator  table ){
        var action = ACTION.column(table);
        return count(action.when(equal("perimeter")).then(action).end());
    }

    private static final OperationColumn countStatusByType (TableDecorator  table, ComparisonExpression op){
        var status = STATUS.column(table);
        return count((status).when(op).then(status).end());
    }


    public static final OperationColumn countStatus200 (TableDecorator table ){
        return countStatusByType(table, equal(200));
    }
    public static final OperationColumn countStatus400 (TableDecorator table){
        return countStatusByType(table, equal(400));
    }

    public static final OperationColumn countStatus401 (TableDecorator table){
        return countStatusByType(table, equal(401));
    }

    public static final OperationColumn countStatus403 (TableDecorator table){
        return countStatusByType(table, equal(403));
    }

    public static final OperationColumn countStatus404 (TableDecorator table){
        return countStatusByType(table, equal(404));
    }

    public static final OperationColumn countStatus500 (TableDecorator table){
        return countStatusByType(table, equal(500));
    }
    public static final OperationColumn countErrorStatus (TableDecorator  table ){
        return countStatusByType(table, greaterOrEqual(400));
    }

    public static final OperationColumn countClientErrorStatus (TableDecorator  table ){
        return countStatusByType(table, greaterOrEqual(400).and(lessThan(500)));
    }
    public static final OperationColumn countServerErrorStatus (TableDecorator  table ){
        return countStatusByType(table, greaterOrEqual(500));
    }

    public static final OperationColumn countSuccesStatus (TableDecorator  table ){
        return countStatusByType(table, greaterOrEqual(200).and(lessOrEqual(226)));
    }

    private static RuntimeException undeclaredColumn(ColumnDecorator column) {
        return new IllegalArgumentException("unknown column " + column);
    }

    public static final ComparisonExpression elapsedTimeExpressions(String name){
        switch(name) {
            case "fastest":     return lessThan(1);
            case "fast":     	return greaterOrEqual(1).and(lessThan(3));
            case "medium":  	return greaterOrEqual(3).and(lessThan(5));
            case "slow":  		return greaterOrEqual(5).and(lessThan(10));
            case "slowest": 	return greaterOrEqual(10);
            default: return null;
        }
    }

    private static final OperationColumn elapsedTimeBySpeed (ComparisonExpression op,TableDecorator table){
        var elapsed = elapsedtime(table);
        return count(elapsed.when(op).then(elapsed).end());
    }
    public static final OperationColumn elapsedTimeVerySlow(TableDecorator table){
        return elapsedTimeBySpeed(elapsedTimeExpressions("slowest"),table);
    }
    public static final OperationColumn elapsedTimeSlow(TableDecorator table){
        return elapsedTimeBySpeed(elapsedTimeExpressions("slow"),table);
    }
    public static final OperationColumn elapsedTimeMedium(TableDecorator table){
        return elapsedTimeBySpeed(elapsedTimeExpressions("medium"),table);
    }
    public static final OperationColumn elapsedTimeFast(TableDecorator table){
        return elapsedTimeBySpeed(elapsedTimeExpressions("fast"),table);
    }
    public static final OperationColumn elapsedTimeFastest(TableDecorator table){
        return elapsedTimeBySpeed(elapsedTimeExpressions("fastest"),table);
    }
}
