package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = MailRequestStageWrapper.class)
public class MailRequestStageWrapper implements EventTrace, Wrapper<MailRequestStage> {
    @Delegate
    @JsonIgnore
    private MailRequestStage stage = new MailRequestStage();

    @Override
    public MailRequestStage unwrap() {
        return stage;
    }
}
