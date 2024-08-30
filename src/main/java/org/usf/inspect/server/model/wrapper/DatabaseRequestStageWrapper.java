package org.usf.inspect.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;

public record DatabaseRequestStageWrapper(long cdRequest, long order,
                                          @JsonIgnore @Delegate DatabaseRequestStage stage) {
}
