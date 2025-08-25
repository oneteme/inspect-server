package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.FtpRequest;

@Getter
@Setter
public class FtpRequestDto extends FtpRequest {
    private ExceptionInfo exception;
}
