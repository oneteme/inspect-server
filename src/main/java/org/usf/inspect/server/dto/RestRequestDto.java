package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.RestRequest;

@Getter
@Setter
public class RestRequestDto extends RestRequest {
    private ExceptionInfo exception;
}
