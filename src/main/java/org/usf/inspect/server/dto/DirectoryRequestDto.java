package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.server.model.DirectoryRequest;

@Getter
@Setter
public class DirectoryRequestDto extends DirectoryRequest {
    private ExceptionTrace exception;
}
