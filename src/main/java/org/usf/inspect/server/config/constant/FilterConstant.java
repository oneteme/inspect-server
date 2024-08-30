package org.usf.inspect.server.config.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.OperationColumn;
import org.usf.jquery.core.QueryContext;
import org.usf.jquery.web.ViewDecorator;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiColumn.COMPLETE;
import static org.usf.jquery.core.ComparisonExpression.*;
import static org.usf.jquery.core.Operator.coalesce;
import static org.usf.jquery.core.Operator.epoch;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterConstant {

    public static DBColumn elapsedtime2(ViewDecorator table) {
        return table.column(END).minus(table.column(START)).epoch();
    }

    private static OperationColumn countStatusByType(ViewDecorator table, ComparisonExpression op) {
        var status = table.column(STATUS);
        return (status).when(op).then(status).end().count();
    }
    public static DBColumn err(ViewDecorator table){ // temporary solution to be changed
        return table.column(ERR_MSG).coalesce(table.column(ERR_TYPE));
    }


    private static OperationColumn countDbBySucces(ViewDecorator table, ComparisonExpression op ){
        var complete = table.column(COMPLETE);
        return (complete).when(op).then(complete).end().count();
    }

    public static OperationColumn countDbError(ViewDecorator table){
        return countDbBySucces(table, eq('F'));
    }

    public static OperationColumn countDbSucces (ViewDecorator table){
        return countDbBySucces(table, eq('T'));
    }

    public static OperationColumn countStatus200(ViewDecorator table) {
        return countStatusByType(table, eq(200));
    }

    public static OperationColumn countStatus400(ViewDecorator table) {
        return countStatusByType(table, eq(400));
    }

    public static OperationColumn countStatus401(ViewDecorator table) {
        return countStatusByType(table, eq(401));
    }

    public static OperationColumn countStatus403(ViewDecorator table) {
        return countStatusByType(table, eq(403));
    }

    public static OperationColumn countStatus404(ViewDecorator table) {
        return countStatusByType(table, eq(404));
    }

    public static OperationColumn countStatus500(ViewDecorator table) {
        return countStatusByType(table, eq(500));
    }

    public static OperationColumn countStatus503(ViewDecorator table) {
        return countStatusByType(table, eq(503));
    }

    public static OperationColumn countErrorStatus(ViewDecorator table) {
        return countStatusByType(table, ge(400));
    }

    public static OperationColumn countClientErrorStatus(ViewDecorator table) {
        return countStatusByType(table, ge(400).and(lt(500)));
    }

    public static OperationColumn countServerErrorStatus(ViewDecorator table) {
        return countStatusByType(table, ge(500));
    }

    public static OperationColumn countSuccesStatus(ViewDecorator table) {
        return countStatusByType(table, ge(200).and(lt(300)));
    }

    public static ComparisonExpression elapsedTimeExpressions(QueryContext ctx, String name) {
        return switch (name) {
            case "fastest" -> lt(1);
            case "fast" -> ge(1).and(lt(3));
            case "medium" -> ge(3).and(lt(5));
            case "slow" -> ge(5).and(lt(10));
            case "slowest" -> ge(10);
            default -> null;
        };
    }

    private static OperationColumn elapsedTimeBySpeed(ComparisonExpression op, ViewDecorator table) {
        var elapsed = elapsedtime2(table);
        return elapsed.when(op).then(elapsed).end().count();
    }

    public static OperationColumn elapsedTimeVerySlow(ViewDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(null,"slowest"), table);
    }

    public static OperationColumn elapsedTimeSlow(ViewDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(null,"slow"), table);
    }

    public static OperationColumn elapsedTimeMedium(ViewDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(null,"medium"), table);
    }

    public static OperationColumn elapsedTimeFast(ViewDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(null,"fast"), table);
    }

    public static OperationColumn elapsedTimeFastest(ViewDecorator table) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(null,"fastest"), table);
    }
}
