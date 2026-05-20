package org.usf.inspect.server.config.constant;

import static org.usf.inspect.server.config.TraceApiColumn.BODY_CONTENT;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_MSG;
import static org.usf.inspect.server.config.TraceApiColumn.ERR_TYPE;
import static org.usf.inspect.server.config.TraceApiColumn.FAILED;
import static org.usf.inspect.server.config.TraceApiColumn.SIZE_IN;
import static org.usf.inspect.server.config.TraceApiColumn.SIZE_OUT;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.STATUS;
import static org.usf.inspect.server.config.TraceApiTable.EXCEPTION;
import static org.usf.inspect.server.config.TraceApiTable.REST_REQUEST;
import static org.usf.jquery.core.ComparisonExpression.eq;
import static org.usf.jquery.core.ComparisonExpression.ge;
import static org.usf.jquery.core.ComparisonExpression.isNotNull;
import static org.usf.jquery.core.ComparisonExpression.isNull;
import static org.usf.jquery.core.ComparisonExpression.lt;

import java.util.Objects;
import java.util.function.Consumer;

import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.jquery.core.AggregateFunction;
import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.QueryBuilder;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.core.ViewColumn;
import org.usf.jquery.web.ViewDecorator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterConstant {

	public static DBColumn percentileElapsed(ViewDecorator table, String... args) {
        return new ViewColumn(null, table.view(), null, null) {
        	
        	@Override
        	public int compose(QueryComposer query, Consumer<DBColumn> groupKeys) {
        		super.compose(query, c-> {}); //declare view only
        		return 1;
        	}
        	
            @Override
            public void build(QueryBuilder query) {
                query.append("PERCENTILE_DISC(0.95) WITHIN GROUP (ORDER BY ")
                		.append("EXTRACT(EPOCH FROM (")
                		.appendViewAlias(getView(), ".dh_end-")
                		.appendViewAlias(getView(), ".dh_str)))");
            }
        };
	}

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

    public static DBColumn performanceTranche(ViewDecorator table, String... args) {
       /* var elapsed = elapsedtime2(table, args);
        var v = table.view();
        return elapsed.toCase()
                .when(lt(1), "0-1s")
                .when(ge(1).and(lt(3)), "1-3s")
                .when(ge(3).and(lt(5)), "3-5s")
                .when(ge(5).and(lt(10)), "5-10s")
                .when(ge(10), "10s+")
                .end();*/
        // Utilise la méthode utilitaire qui accepte des bornes en secondes
        return performanceTrancheBuild(table, 1, 3, 5, 10);
    }

    public static DBColumn performanceTranche2(ViewDecorator table, String... args) {
       return performanceTrancheBuild(table, 5, 10);
    }

    public static DBColumn sizeTranche(ViewDecorator table, String... args) {
        return sizeTrancheBuild(table, 100, 200, 300);
    }

    // Méthode utilitaire factorisée : construit un CASE SQL sur la durée
    // EXTRACT(EPOCH FROM (v.dh_end - v.dh_str)) en utilisant les bornes fournies (en secondes).
    // Semantique :
    //  - première tranche : < bounds[0] -> label '0-1s' si bounds[0]==1 sinon '<Ns'
    //  - tranches intermédiaires : >=prev and <cur -> label 'prev-cur s'
    //  - tranche finale : >= last -> label '>Ns'
    private static DBColumn performanceTrancheBuild(ViewDecorator table, int... bounds) {
        var v = table.view();
        return new ViewColumn(null, null, null, null) {
            @Override
            public void build(QueryBuilder query) {
                query.append("case");
                if (bounds == null || bounds.length == 0) {
                    query.append(" end");
                    return;
                }
                // première tranche
                int first = bounds[0];
                String firstLabel = "1";
                query.append(" when (")
                        .append(" EXTRACT(EPOCH FROM (")
                        .appendViewAlias(v, ".dh_end-")
                        .appendViewAlias(v, ".dh_str))<")
                        .append(String.valueOf(first))
                        .append(") then '")
                        .append(firstLabel)
                        .append("'");

                // tranches intermédiaires
                for (int i = 1; i < bounds.length; i++) {
                    int prev = bounds[i - 1];
                    int cur = bounds[i];
                    String label = "" + (i + 1);
                    query.append(" when (")
                            .append(" EXTRACT(EPOCH FROM (")
                            .appendViewAlias(v, ".dh_end-")
                            .appendViewAlias(v, ".dh_str))>=")
                            .append(String.valueOf(prev))
                            .append(" and ")
                            .append(" EXTRACT(EPOCH FROM (")
                            .appendViewAlias(v, ".dh_end-")
                            .appendViewAlias(v, ".dh_str))<")
                            .append(String.valueOf(cur))
                            .append(") then '")
                            .append(label)
                            .append("'");
                }

                // tranche finale : >= last
                int last = bounds[bounds.length - 1];
                String lastLabel = "" + (bounds.length + 1);
                query.append(" when (")
                        .append(" EXTRACT(EPOCH FROM (")
                        .appendViewAlias(v, ".dh_end-")
                        .appendViewAlias(v, ".dh_str))>=")
                        .append(String.valueOf(last))
                        .append(") then '")
                        .append(lastLabel)
                        .append("'");

                query.append(" end");
            }
        };
    }

    private static DBColumn sizeTrancheBuild(ViewDecorator table, int... bounds) {
        var v = table.view();
        return new ViewColumn(null, null, null, null) {
            @Override
            public void build(QueryBuilder query) {
                query.append("case");
                if (bounds == null || bounds.length == 0) {
                    query.append(" end");
                    return;
                }
                // première tranche
                int first = bounds[0];
                String firstLabel = "1";
                query.append(" when (")
                        .appendViewAlias(v, ".va_i_sze<")
                        .append(String.valueOf(first))
                        .append(" or ")
                        .appendViewAlias(v, ".va_o_sze<")
                        .append(String.valueOf(first))
                        .append(") then '")
                        .append(firstLabel)
                        .append("'");

                // tranches intermédiaires
                for (int i = 1; i < bounds.length; i++) {
                    int prev = bounds[i - 1];
                    int cur = bounds[i];
                    String label = "" + (i + 1);
                    query.append(" when ((")
                            .appendViewAlias(v, ".va_i_sze>=")
                            .append(String.valueOf(prev))
                            .append(" and ")
                            .appendViewAlias(v, ".va_i_sze<")
                            .append(String.valueOf(cur))
                            .append(") or (")
                            .appendViewAlias(v, ".va_o_sze>=")
                            .append(String.valueOf(prev))
                            .append(" and ")
                            .appendViewAlias(v, ".va_o_sze<")
                            .append(String.valueOf(cur))
                            .append(")) then '")
                            .append(label)
                            .append("'");
                }

                // tranche finale : >= last
                int last = bounds[bounds.length - 1];
                String lastLabel = "" + (bounds.length + 1);
                query.append(" when (")
                        .appendViewAlias(v, ".va_i_sze>=")
                        .append(String.valueOf(last))
                        .append(" or ")
                        .appendViewAlias(v, ".va_o_sze>=")
                        .append(String.valueOf(last))
                        .append(") then '")
                        .append(lastLabel)
                        .append("'");

                query.append(" end");
            }
        };
    }

    public static DBColumn statusTranche(ViewDecorator table, String... args) {
        var v = table.view();
        return new ViewColumn(null, null, null, null){
            @Override
            public void build(QueryBuilder query) {
                query.append("case")
                        .append(" when (")
                        .appendViewAlias(v, ".cd_stt=0) then '1'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<200) then '2'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<300) then '3'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<400) then '4'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<500) then '5'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt>=500) then '6'")
                        .append(" end");

            }
        };
    }

    public static DBColumn statusOkClientServerError(ViewDecorator table, String... args) {
        var v = table.view();
        return new ViewColumn(null, null, null, null){
            @Override
            public void build(QueryBuilder query) {
                query.append("case")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<300) then 'OK'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt<500) then 'ClientError'")
                        .append(" when(")
                        .appendViewAlias(v, ".cd_stt>=500) then 'ServerError'")
                        .append(" end");

            }
        };
    }

    public static DBColumn elapsedtime_by_args(ViewDecorator table, String ... args){ //(v1,v2) , (v1) , (null,v2)
        var elapsed = elapsedtime2(table, args);

        boolean hasMin = args.length > 0 && !Objects.equals(args[0], "null") && !args[0].isEmpty();
        boolean hasMax = args.length > 1 && !Objects.equals(args[1], "null") && !args[1].isEmpty();
        ComparisonExpression condition;
        if (hasMin && hasMax) {
            int minValue = Integer.parseInt(args[0]);
            int maxValue = Integer.parseInt(args[1]);
            condition = ge(minValue).and(lt(maxValue));
        } else if (hasMin) {
            int minValue = Integer.parseInt(args[0]);
            condition = ge(minValue);
        } else if (hasMax) {
            int maxValue = Integer.parseInt(args[1]);
            condition = lt(maxValue);
        } else {
            return elapsed;
        }

        return elapsed.toCase().when(condition, elapsed).end().count();
    }

    public static DBColumn StatusByType(ViewDecorator table, String... args) {
        var status = table.column(STATUS);
        boolean hasMin = args.length > 0 && !Objects.equals(args[0], "null") && !args[0].isEmpty();
        boolean hasMax = args.length > 1 && !Objects.equals(args[1], "null") && !args[1].isEmpty();
        ComparisonExpression condition;
        if (hasMin && hasMax) {
            int minValue = Integer.parseInt(args[0]);
            int maxValue = Integer.parseInt(args[1]);
            condition = ge(minValue).and(lt(maxValue));
        } else if (hasMin) {
            int minValue = Integer.parseInt(args[0]);
            condition = ge(minValue);
        } else if (hasMax) {
            int maxValue = Integer.parseInt(args[1]);
            condition = lt(maxValue);
        } else {
            return status;
        }
        return status.toCase()
                .when(condition, status).end();
    }

    private static DBColumn elapsedTimeBySpeed(ComparisonExpression op, ViewDecorator table, String... args) {
        var elapsed = elapsedtime2(table, args);
        return elapsed.toCase().when(op, elapsed).end().count();
    }

    public static DBColumn sizeIn(ViewDecorator table, String... args) {
        var sizeIn = table.column(SIZE_IN);
        return sizeIn.toCase().when(eq(-1), 0).orElse(sizeIn);
    }

    public static DBColumn sizeOut(ViewDecorator table, String... args) {
        var sizeOut = table.column(SIZE_OUT);
        return sizeOut.toCase().when(eq(-1), 0).orElse(sizeOut);
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
