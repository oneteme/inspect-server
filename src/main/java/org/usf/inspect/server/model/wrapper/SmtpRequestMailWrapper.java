package org.usf.inspect.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.Mail;

public record SmtpRequestMailWrapper(long cdRequest, @JsonIgnore @Delegate Mail mail) {
}
