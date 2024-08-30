package org.usf.inspect.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.FtpRequestStage;

public record FtpRequestStageWrapper(long cdRequest, long order,
                                     @JsonIgnore @Delegate FtpRequestStage stage) {
}
