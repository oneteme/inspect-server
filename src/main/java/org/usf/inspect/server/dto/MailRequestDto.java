package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.server.model.MailRequest;

@Getter
@Setter
public class MailRequestDto extends MailRequest {
    private ExceptionTrace exception;
}
