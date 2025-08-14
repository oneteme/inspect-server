package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.DirectoryRequest;
import org.usf.inspect.core.ExceptionInfo;

@Getter
@Setter
public class DirectoryRequestDto extends DirectoryRequest {
    private ExceptionInfo exception;
}
