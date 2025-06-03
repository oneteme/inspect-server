package org.usf.inspect.server.config.constant;

import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.jquery.core.ComparisonExpression.*;

import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.web.Environment;
import org.usf.jquery.web.ViewDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterConstant {

    public static DBColumn elapsedtime2(ViewDecorator table, Environment env, String... args) {
        return table.column(END).minus(table.column(START)).epoch();
    }

    private static DBColumn countStatusByType(ViewDecorator table, ComparisonExpression op) {
        var status = table.column(STATUS);
        return (status).beginCase().when(op, status).end().count();
    }

    public static DBColumn countExceptions(ViewDecorator table, Environment env, String... args){
        return table.column(ERR_MSG).beginCase().when(isNotNull(),1).orElse(0).sum();
    }

    public static DBColumn countNoExceptions(ViewDecorator table, Environment env, String... args){
        return table.column(ERR_MSG).beginCase().when(isNull(),1).orElse(0).sum();
    }

    public static DBColumn err(ViewDecorator table, Environment env, String... args){ // temporary solution to be changed
        return table.column(ERR_MSG).coalesce(table.column(ERR_TYPE));
    }

    public static DBColumn countError(ViewDecorator table, Environment env, String... args){
        return countStatusByType(table, eq(false));
    }

    public static DBColumn countSuccess(ViewDecorator table, Environment env, String... args){
        return countStatusByType(table, eq(true));
    }

    public static DBColumn countStatus200(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(200));
    }

    public static DBColumn countStatus400(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(400));
    }

    public static DBColumn countStatus401(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(401));
    }

    public static DBColumn countStatus403(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(403));
    }

    public static DBColumn countStatus404(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(404));
    }

    public static DBColumn countStatus500(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(500));
    }

    public static DBColumn countStatus503(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(503));
    }

    public static DBColumn countErrorStatus(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, ge(400));
    }

    public static DBColumn countClientErrorStatus(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, ge(400).and(lt(500)));
    }

    public static DBColumn countServerErrorStatus(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, ge(500));
    }

    public static DBColumn countServerUnavailableStatus(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, eq(0));
    }

    public static DBColumn countSuccesStatus(ViewDecorator table, Environment env, String... args) {
        return countStatusByType(table, ge(200).and(lt(300)));
    }
    public static ComparisonExpression elapsedTimeExpressions(ViewDecorator table, Environment env, String name) {
        return switch (name) {
            case "fastest" -> lt(1);
            case "fast" -> ge(1).and(lt(3));
            case "medium" -> ge(3).and(lt(5));
            case "slow" -> ge(5).and(lt(10));
            case "slowest" -> ge(10);
            default -> null;
        };
    }

    private static DBColumn elapsedTimeBySpeed(ComparisonExpression op, ViewDecorator table, Environment env, String... args) {
        var elapsed = elapsedtime2(table, env, args);
        return elapsed.beginCase().when(op, elapsed).end().count();
    }

    public static DBColumn elapsedTimeVerySlow(ViewDecorator table, Environment env, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, env, "slowest"), table, env, args);
    }

    public static DBColumn elapsedTimeSlow(ViewDecorator table, Environment env, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, env, "slow"), table, env, args);
    }

    public static DBColumn elapsedTimeMedium(ViewDecorator table, Environment env, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, env, "medium"), table, env, args);
    }

    public static DBColumn elapsedTimeFast(ViewDecorator table, Environment env, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, env, "fast"), table, env, args);
    }

    public static DBColumn elapsedTimeFastest(ViewDecorator table, Environment env, String... args) {
        return elapsedTimeBySpeed(elapsedTimeExpressions(table, env, "fastest"), table, env, args);
    }
}
