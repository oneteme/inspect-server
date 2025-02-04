package org.usf.inspect.server.config;

import static java.util.Objects.nonNull;

import org.usf.inspect.server.config.constant.FilterConstant;
import org.usf.jquery.core.ComparisonExpression;
import org.usf.jquery.web.ColumnBuilder;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.CriteriaBuilder;
import org.usf.jquery.web.ViewDecorator;

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
    SIZE("size"),
    SUBJECT("subject"),
    FROM("from"),
    REPLY_TO("replyTo"),
    RECIPIENTS("recipients"),
    CONTENT_ENCODING_IN("contentEncodingIn"),
    CONTENT_ENCODING_OUT("contentEncodingOut"),
    START("start"),
    END("end"),
    THREAD("thread"),
    API_NAME("apiName"), //API
    APP_NAME("appName"), //APP
    USER("user"),
    CACHE_CONTROL("cacheControl"),
    USER_AGT("userAgent"),
    MASK("mask"),
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
    SCHEMA("schema"),
    DRIVER("driver"),
    DB_NAME("dbName"),
    DB_VERSION("dbVersion"),
    COMMANDS("commands"),
    COMMAND("command"),
    ACTION_COUNT("actionCount"),
    ARG("arg"),
    PARENT("parent"),
    SERVER_VERSION("serverVersion"),
    CLIENT_VERSION("clientVersion"),
    REMOTE("remote"),

    INSTANCE_ENV("instance"),

    ORDER("order"),
    COLLECTOR("collector"),
    BRANCH("branch"),
    HASH("hash"),
    //---
    ELAPSEDTIME("elapsedtime", FilterConstant::elapsedtime2, CriteriaBuilder.multiArgs(FilterConstant::elapsedTimeExpressions)),
    COUNT_SLOWEST("elapsedTimeSlowest", FilterConstant::elapsedTimeVerySlow),
    COUNT_SLOW("elapsedTimeSlow", FilterConstant::elapsedTimeSlow),
    COUNT_MEDIUM("elapsedTimeMedium", FilterConstant::elapsedTimeMedium),
    COUNT_FAST("elapsedTimeFast", FilterConstant::elapsedTimeFast),
    COUNT_FASTEST("elapsedTimeFastest", FilterConstant::elapsedTimeFastest),
    COUNT_ERROR("countErrorRows", FilterConstant::countErrorStatus),

    COUNT_ERROR_CLIENT("countClientErrorRows", FilterConstant::countClientErrorStatus),
    COUNT_ERROR_SERVER("countServerErrorRows", FilterConstant::countServerErrorStatus),
    COUNT_UNAVAILABLE_SERVER("countServerUnavailableRows", FilterConstant::countServerUnavailableStatus),
    COUNT_SUCCES("countSuccesRows", FilterConstant::countSuccesStatus),
    COUNT_200("count200", FilterConstant::countStatus200), //set type improve perf
    COUNT_400("count400", FilterConstant::countStatus400),
    COUNT_401("count401", FilterConstant::countStatus401),
    COUNT_403("count403", FilterConstant::countStatus403),
    COUNT_404("count404", FilterConstant::countStatus404),
    COUNT_500("count500", FilterConstant::countStatus500),
    COUNT_503("count503", FilterConstant::countStatus503),
    COUNT_REQUEST_ERROR("countRequestError", FilterConstant::countError),
    COUNT_REQUEST_SUCCESS("countRequestSuccess", FilterConstant::countSuccess),
    ERR("err", FilterConstant::err),
    COUNT_EXCEPTION("countException", FilterConstant::countExceptions); //isNull
	
    private final String out; //nullable
    private final ColumnBuilder columnTemplate;
    private final CriteriaBuilder<ComparisonExpression> expressionFn;

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
    
    public String reference() {
        return this.out; //suppose that use same ref for all view
    }
    
    @Override
    public String reference(ViewDecorator vd) {
        return reference();
    }

    @Override
    public ColumnBuilder builder(ViewDecorator vd) {
        return nonNull(columnTemplate)
                ? columnTemplate
                : ColumnDecorator.super.builder(vd);
    }
    
    @Override
    public CriteriaBuilder<ComparisonExpression>criteria(String name) {
        return "group".equals(name) && nonNull(expressionFn)
                ? expressionFn
                : ColumnDecorator.super.criteria(name);
    }

}
