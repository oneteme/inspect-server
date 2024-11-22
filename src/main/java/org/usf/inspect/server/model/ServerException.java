package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;

public record ServerException(long cdRequest, Long order, ExceptionInfo exceptionInfo) {}
