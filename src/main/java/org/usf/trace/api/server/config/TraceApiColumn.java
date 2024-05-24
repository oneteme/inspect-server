package org.usf.trace.api.server.config;

import static java.util.Objects.nonNull;

import org.usf.jquery.web.ColumnBuilder;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.CriteriaBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiColumn implements ColumnDecorator {

    ID("id"),
    METHOD("method"), //METHOD
    PROTOCOL("protocol"),
    HOST("host"),
    PORT("port"),
    PATH("path"),
    QUERY("query"),
    MEDIA("contentType"), //mime | media type
    AUTH("auth"),
    STATUS("status"),
    SIZE_IN("sizeIn"),
    SIZE_OUT("sizeOut"),
    START("start"),
    END("end"),
    THREAD("thread"),
    API_NAME("apiName"), //API
    APP_NAME("appName"), //APP
    USER("user"),
    VERSION("version"),
    ADDRESS("address"),
    ENVIRONEMENT("environement"), //ENV
    OS("os"),
    RE("re"),

    NAME("name"),
    TYPE("type"),
    LOCATION("location"),
    ERR_TYPE("errorType"),
    ERR_MSG("errorMessage"),
    DB("db"),
    DRIVER("driver"),
    DB_NAME("dbName"),
    DB_VERSION("dbVersion"),
    COMPLETE("complete"),
    COMMANDS("commands"),
    ACTION_COUNT("actionCount"),
    PARENT("parent"),
    //---
    ELAPSEDTIME("elapsedtime", DataConstants::elapsedtime2, DataConstants::elapsedTimeExpressions),
    COUNT_SLOWEST("elapsedTimeSlowest", DataConstants::elapsedTimeVerySlow),
    COUNT_SLOW("elapsedTimeSlow", DataConstants::elapsedTimeSlow),
    COUNT_MEDIUM("elapsedTimeMedium", DataConstants::elapsedTimeMedium),
    COUNT_FAST("elapsedTimeFast", DataConstants::elapsedTimeFast),
    COUNT_FASTEST("elapsedTimeFastest", DataConstants::elapsedTimeFastest),
    COUNT_ERROR("countErrorRows", DataConstants::countErrorStatus),

    COUNT_ERROR_CLIENT("countClientErrorRows", DataConstants::countClientErrorStatus),
    COUNT_ERROR_SERVER("countServerErrorRows", DataConstants::countServerErrorStatus),
    COUNT_SUCCES("countSuccesRows", DataConstants::countSuccesStatus),
    COUNT_200("count200", DataConstants::countStatus200), //set type improve perf
    COUNT_400("count400", DataConstants::countStatus400),
    COUNT_401("count401", DataConstants::countStatus401),
    COUNT_403("count403", DataConstants::countStatus403),
    COUNT_404("count404", DataConstants::countStatus404),
    COUNT_500("count500", DataConstants::countStatus500),
    COUNT_503("count503", DataConstants::countStatus503),
    COUNT_DB_ERROR("countDbError", DataConstants::countDbError),
    COUNT_DB_SUCCES("countDbSucces", DataConstants::countDbSucces);


    private final String out; //nullable
    private final ColumnBuilder columnTemplate;
    private final CriteriaBuilder<String> expressionFn;

    TraceApiColumn(@NonNull String tagname) {
        this(tagname, null, null);
    }

    TraceApiColumn(@NonNull String tagname, @NonNull ColumnBuilder columnTemplate) {
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
        return nonNull(columnTemplate)
                ? columnTemplate
                : ColumnDecorator.super.builder();
    }

    @Override
    public CriteriaBuilder<String> criteria(String name) {

        return "group".equals(name) && nonNull(expressionFn)
                ? expressionFn
                : ColumnDecorator.super.criteria(name);
    }

}
