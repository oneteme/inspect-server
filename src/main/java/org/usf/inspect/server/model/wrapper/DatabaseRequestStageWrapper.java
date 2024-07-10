package org.usf.inspect.server.model.wrapper;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;

@Setter
@Getter
@RequiredArgsConstructor
public class DatabaseRequestStageWrapper {
    private final long cdRequest;
    private final long order;
    @Delegate
    private final DatabaseRequestStage stage;
}
