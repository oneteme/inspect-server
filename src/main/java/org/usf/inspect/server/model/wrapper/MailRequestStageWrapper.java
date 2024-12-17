package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MailRequestStage;

@Getter
@Setter
@RequiredArgsConstructor
public class MailRequestStageWrapper {
    private long id;
    private int order;

    @JsonIgnore
    @Delegate
    private final MailRequestStage mailRequestStage;
}
