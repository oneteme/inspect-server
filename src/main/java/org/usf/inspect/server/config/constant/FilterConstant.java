package org.usf.inspect.server.config.constant;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiTable.EXCEPTION;
import static org.usf.inspect.server.config.TraceApiTable.REST_REQUEST;
import static org.usf.jquery.core.ComparisonExpression.eq;
import static org.usf.jquery.core.ComparisonExpression.ge;
import static org.usf.jquery.core.ComparisonExpression.isNotNull;
import static org.usf.jquery.core.ComparisonExpression.isNull;
import static org.usf.jquery.core.ComparisonExpression.lt;
import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.web.ViewDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterConstant {

    public static DBColumn elapsedtime2(ViewDecorator table, String... args) {
        return table.column(END).minus(table.column(START)).epoch();
    }

    private static DBColumn countStatusByType(ViewDecorator table, TraceApiColumn column, ComparisonExpression op) {
        var status = table.column(column);
        return (status).toCase().when(op, status).end().count();
    }

    public static DBColumn countExceptions(ViewDecorator table, String... args){
        return table.column(ERR_TYPE).toCase().when(isNotNull(),1).orElse(0).sum();
    }

    public static DBColumn countExceptionsRest(ViewDecorator table, String... args){
        return table.column(ERR_TYPE).toCase().when(isNotNull(),1).orElse(REST_REQUEST.column(BODY_CONTENT).toCase().when(isNotNull(), 1).orElse(0)).sum();
    }

    public static DBColumn countNoExceptions(ViewDecorator table, String... args){
        return table.column(ERR_TYPE).toCase().when(isNull(),1).orElse(0).sum();
    }

    public static DBColumn err(ViewDecorator table, String... args){ // temporary solution to be changed
        return table.column(ERR_MSG).coalesce(table.column(ERR_TYPE));
    }

    public static DBColumn countError(ViewDecorator table, String... args){
        return countStatusByType(table, FAILED, eq(true));
    }

    public static DBColumn countSuccess(ViewDecorator table, String... args){
        return countStatusByType(table, FAILED, eq(false));
    }

    public static DBColumn countStatus200(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(200));
    }

    public static DBColumn countStatus400(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(400));
    }

    public static DBColumn countStatus401(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(401));
    }

    public static DBColumn countStatus403(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(403));
    }

    public static DBColumn countStatus404(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(404));
    }

    public static DBColumn countStatus500(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(500));
    }

    public static DBColumn countStatus503(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(503));
    }

    public static DBColumn countErrorStatus(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, ge(400));
    }

    public static DBColumn countClientErrorStatus(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, ge(400).and(lt(500)));
    }

    public static DBColumn countServerErrorStatus(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, ge(500));
    }

    public static DBColumn countServerUnavailableStatus(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, eq(0));
    }

    public static DBColumn countSuccesStatus(ViewDecorator table, String... args) {
        return countStatusByType(table, STATUS, ge(200).and(lt(300)));
    }
    public static ComparisonExpression elapsedTimeExpressions(ViewDecorator table, String name) {
        return switch (name) {
            case "fastest" -> lt(1);
            case "fast" -> ge(1).and(lt(3));
            case "medium" -> ge(3).and(lt(5));
            case "slow" -> ge(5).and(lt(10));
            case "slowest" -> ge(10);
            default -> null;
        };
    }

    public static DBColumn errorTypeExpressions(ViewDecorator table, String... args) {
        var status = table.column(STATUS);
        return status.toCase()
                .when(eq(0), EXCEPTION.column(ERR_TYPE))
                .when(ge(200).and(lt(400)), null)
                .when(ge(400).and(lt(500)), "ClientError")
                .when(ge(500), "ServerError")
                .end();
    }

    public static DBColumn size_In_args(ViewDecorator table, String ... args){ //(v1,v2) , (v1) , (null,v2)
        var sizeIn = table.column(SIZE_IN);
        return sizeIn.toCase().when(ge(args[0]).and(lt(args[1])), sizeIn).end();
    }

    private static DBColumn elapsedTimeBySpeed(ComparisonExpression op, ViewDecorator table, String... args) {
        var elapsed = elapsedtime2(table, args);
        return elapsed.toCase().when(op, elapsed).end().count();
    }

    public static DBColumn sizeIn(ViewDecorator table, String... args) {
        var sizeIn = table.column(SIZE_IN);
        return sizeIn.toCase().when(eq(-1), 0).orElse(sizeIn).avg();
    }

    public static DBColumn sizeOut(ViewDecorator table, String... args) {
        var sizeOut = table.column(SIZE_OUT);
        return sizeOut.toCase().when(eq(-1), 0).orElse(sizeOut).avg();
    }

    public static DBColumn elapsedTimeVerySlow(ViewDecorator table, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, "slowest"), table, args);
    }

    public static DBColumn elapsedTimeSlow(ViewDecorator table, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, "slow"), table, args);
    }

    public static DBColumn elapsedTimeMedium(ViewDecorator table, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, "medium"), table, args);
    }

    public static DBColumn elapsedTimeFast(ViewDecorator table, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, "fast"), table, args);
    }

    public static DBColumn elapsedTimeFastest(ViewDecorator table, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, "fastest"), table, args);
    }
}
