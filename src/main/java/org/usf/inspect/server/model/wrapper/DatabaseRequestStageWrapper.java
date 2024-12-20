package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.DatabaseRequestStage;

@Getter
@Setter
@RequiredArgsConstructor
public class DatabaseRequestStageWrapper {
    private int order;

    @JsonIgnore
    @Delegate
    private final DatabaseRequestStage databaseRequest;
}
