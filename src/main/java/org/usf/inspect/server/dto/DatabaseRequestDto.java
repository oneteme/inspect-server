package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.server.model.DatabaseRequest;

@Getter
@Setter
public class DatabaseRequestDto extends DatabaseRequest {
    private ExceptionInfo exception;
}
