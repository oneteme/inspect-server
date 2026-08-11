package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.server.model.FtpRequest;

/**
 * Represents an FTP request DTO enriched with exception details.
 */
@Getter
@Setter
public class FtpRequestDto extends FtpRequest {
    private ExceptionInfo exception;
}
