package org.usf.trace.api.server.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.TaggableColumn;
import org.usf.jquery.web.ArgumentParser;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;
import org.usf.jquery.web.TableMetadata;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.function.Function;

import static org.usf.jquery.core.DBFunction.count;

@RequiredArgsConstructor
public enum TraceApiColumn implements ColumnDecorator {

    ID_INCOMING_REQ("idIncomingRequest"),
    MTH("mth"),
    PROTOCOL("protocol"),
    HOST("host"),
    PORT("port"),
    PATH("path"),
    QUERY("query"),
    CONTENT_TYPE("contentType"),
    AUTH("auth"),
    STATUS("status"),
    SIZE_IN("sizeIn"),
    SIZE_OUT("sizeOut"),
    START_DATETIME("startDatetime", null, DataConstants::greaterOrEqualsExpressions),
    FINISH_DATETIME("finishDatetime", null, DataConstants::lessThanExpressions),
    THREAD("thread"),
    API_NAME("apiName"),
    USER("user"),
    APP_NAME("appName"),
    VERSION("version"),
    ADDRESS("address"),
    ENVIRONEMENT("environement"),
    OS("os"),
    RE("re"),





    //---
    AS_DATE("asDate", DataConstants::asDate),
    BY_DAY("byDay", DataConstants::byDay),

    BY_MONTH("byMonth", DataConstants::byMonth),
    BY_YEAR("byYear", DataConstants::byYear),
    ELAPSEDTIME("elapsedtime", DataConstants::elapsedtime, DataConstants::elapsedTimeExpressions),
    AVG_ELAPSEDTIME("avgElapsedTime", DataConstants::avgElapsedTime),
    MAX_ELAPSEDTIME("maxElapsedTime", DataConstants::maxElapsedTime),
    MIN_ELAPSEDTIME("minElapsedTime", DataConstants::minElapsedTime),

    COUNT_ELAPSEDTIME_SLOWEST("elapsedTimeSlowest", DataConstants::elapsedTimeVerySlow),
    COUNT_ELAPSEDTIME_SLOW("elapsedTimeSlow", DataConstants::elapsedTimeSlow),
    COUNT_ELAPSEDTIME_MEDIUM("elapsedTimeMedium", DataConstants::elapsedTimeMedium),
    COUNT_ELAPSEDTIME_FAST("elapsedTimeFast", DataConstants::elapsedTimeFast),
    COUNT_ELAPSEDTIME_FASTEST("elapsedTimeFastest", DataConstants::elapsedTimeFastest),
    COUNT("countRows", t -> count(), DataConstants::elapsedTimeExpressions),
    COUNT_STATUS_ERROR("countErrorRows", DataConstants::countErrorStatus),

    COUNT_STATUS_ERROR_CLIENT("countClientErrorRows", DataConstants::countClientErrorStatus),
    COUNT_STATUS_ERROR_SERVER("countServerErrorRows", DataConstants::countServerErrorStatus),
    COUNT_STATUS_SUCCES("countSuccesRows", DataConstants::countSuccesStatus),
    COUNT_200("count200", DataConstants::countStatus200),
    COUNT_400("count400", DataConstants::countStatus400),
    COUNT_401("count401", DataConstants::countStatus401),
    COUNT_403("count403", DataConstants::countStatus403),
    COUNT_404("count404", DataConstants::countStatus404),
    COUNT_500("count500", DataConstants::countStatus500);


    private final String out; //nullable
    private final Function<TableDecorator, DBColumn> columnTemplate;
    private final Function<String, ComparisonExpression> expressionFn;

    private TraceApiColumn(@NonNull String tagname) {
        this(tagname, null, null);
    }

    private TraceApiColumn(@NonNull String tagname, @NonNull Function<TableDecorator, DBColumn> columnTemplate) {
        this(tagname, columnTemplate, null);
    }

    @Override
    public String identity() {
        return this.name();
    }

    @Override
    public String reference() {
        return this.out;
    }

    @Override
    public TaggableColumn column(TableDecorator table) {
        return columnTemplate == null
                ? ColumnDecorator.super.column(table)
                : columnTemplate.apply(table).as(out);
    }
    /*@Override
    public DBFilter filter(TableDecorator table, TableMetadata meta, String... values) {
        System.out.println("-------------"+values.toString());
        if (expressionFn != null) {
            var list = new ArrayList<ComparisonExpression>(values.length);
            for (var v : values) {
                var exp = expressionFn.apply(v);
                if (exp == null) {
                    break;
                }
                list.add(exp);
            }
            if (list.size() == values.length) { // all in
                return column(table).filter(list.stream().reduce(ComparisonExpression::or).orElseThrow());
            }
        }
        return  ColumnDecorator.super.column(table).filter().
    }*/


    @Override
    public ComparisonExpression expression(TableMetadata cm, String... values) {
        if (expressionFn != null) {
            var list = new ArrayList<ComparisonExpression>(values.length);
            for (var v : values) {
                var exp = expressionFn.apply(v);
                if (exp == null) {
                    break;
                }
                list.add(exp);
            }
            if (list.size() == values.length) { // all values or not
                return list.stream().reduce(ComparisonExpression::or).orElseThrow();
            }
        }
        return ColumnDecorator.super.expression(cm, values);
    }


    @Override
    public ArgumentParser parser(TableMetadata metadata) {
        if (this == COUNT) {
            return Integer::parseInt;
        }
        if (this == BY_DAY || this == BY_YEAR || this == BY_MONTH) {
            return Integer::parseInt;
        }
        if (this == AS_DATE) {
            return v -> Date.valueOf(LocalDate.parse(v));
        }
        return ColumnDecorator.super.parser(metadata);
    }
}
