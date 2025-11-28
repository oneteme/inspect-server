package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.server.model.MailRequest;

@Getter
@Setter
public class MailRequestDto extends MailRequest {
    private ExceptionInfo exception;
}
