package org.usf.trace.api.server.config;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;

import org.usf.jquery.core.DBColumn;
import org.usf.jquery.web.ColumnBuilder;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.CriteriaBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiColumn implements ColumnDecorator {

    ID_INCOMING_REQ("idIncomingRequest"),
    MTH("mth"), //METHOD
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
    START_DATETIME("startDatetime", null, DataConstants::greaterOrEqualsExpressions),//@see gt, lt, .. //start
    FINISH_DATETIME("finishDatetime", null, DataConstants::lessThanExpressions), //end
    THREAD("thread"),
    API_NAME("apiName"), //API
    USER("user"),
    APP_NAME("appName"), //APP
    VERSION("version"),
    ADDRESS("address"),
    ENVIRONEMENT("environement"), //ENV
    OS("os"),
    RE("re"),





    //---
    @Deprecated(forRemoval = true)
    AS_DATE("asDate", DataConstants::asDate), //start.date
    @Deprecated(forRemoval = true)
    BY_DAY("byDay", DataConstants::byDay), //start.day

    @Deprecated(forRemoval = true)
    BY_MONTH("byMonth", DataConstants::byMonth), //start.month
    @Deprecated(forRemoval = true)
    BY_YEAR("byYear", DataConstants::byYear), //start.year
    ELAPSEDTIME("elapsedtime", DataConstants::elapsedtime, DataConstants::elapsedTimeExpressions),
    @Deprecated(forRemoval = true)
    AVG_ELAPSEDTIME("avgElapsedTime", DataConstants::avgElapsedTime), //elapsedtime.avg
    @Deprecated(forRemoval = true)
    MAX_ELAPSEDTIME("maxElapsedTime", DataConstants::maxElapsedTime), //elapsedtime.max
    @Deprecated(forRemoval = true)
    MIN_ELAPSEDTIME("minElapsedTime", DataConstants::minElapsedTime), //elapsedtime.min

    COUNT_ELAPSEDTIME_SLOWEST("elapsedTimeSlowest", DataConstants::elapsedTimeVerySlow),
    COUNT_ELAPSEDTIME_SLOW("elapsedTimeSlow", DataConstants::elapsedTimeSlow),
    COUNT_ELAPSEDTIME_MEDIUM("elapsedTimeMedium", DataConstants::elapsedTimeMedium),
    COUNT_ELAPSEDTIME_FAST("elapsedTimeFast", DataConstants::elapsedTimeFast),
    COUNT_ELAPSEDTIME_FASTEST("elapsedTimeFastest", DataConstants::elapsedTimeFastest),
    @Deprecated(forRemoval = true)
    COUNT("countRows", t-> DBColumn.count(), DataConstants::elapsedTimeExpressions), //count
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
    private final ColumnBuilder columnTemplate;
    private final CriteriaBuilder<String> expressionFn;

    private TraceApiColumn(@NonNull String tagname) {
        this(tagname, null, null);
    }

    private TraceApiColumn(@NonNull String tagname, @NonNull ColumnBuilder columnTemplate) {
        this(tagname, columnTemplate, null);
    }

    @Override
    public String identity() {
        return this.name().toLowerCase();
    }

    @Override
    public String reference() {
        return this.out;
    }

    @Override
    public ColumnBuilder builder() {
    	return requireNonNullElseGet(columnTemplate, ColumnDecorator.super::builder);
    }
    
    @Override
    public CriteriaBuilder<String> criteria(String name) {
    	
    	return "group".equals(name) && nonNull(expressionFn) //TODO rename group
    			? expressionFn
    			: ColumnDecorator.super.criteria(name);
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


//    @Override
//    public ComparisonExpression expression(TableMetadata cm, String... values) {
//        if (expressionFn != null) {
//            var list = new ArrayList<ComparisonExpression>(values.length);
//            for (var v : values) {
//                var exp = expressionFn.apply(v);
//                if (exp == null) {
//                    break;
//                }
//                list.add(exp);
//            }
//            if (list.size() == values.length) { // all values or not
//                return list.stream().reduce(ComparisonExpression::or).orElseThrow();
//            }
//        }
//        return ColumnDecorator.super.expression(cm, values);
//    }


//    @Override
//    public ArgumentParser parser(TableMetadata metadata) {
//        if (this == COUNT) {
//            return Integer::parseInt;
//        }
//        if (this == BY_DAY || this == BY_YEAR || this == BY_MONTH) {
//            return Integer::parseInt;
//        }
//        if (this == AS_DATE) {
//            return v -> Date.valueOf(LocalDate.parse(v));
//        }
//        return ColumnDecorator.super.parser(metadata);
//    }
}
