package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DatabaseRequest;
import org.usf.inspect.core.ExceptionInfo;

@Getter
@Setter
public class DatabaseRequestDto extends DatabaseRequest {
    private ExceptionInfo exception;
}
