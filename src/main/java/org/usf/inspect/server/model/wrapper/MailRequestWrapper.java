package org.usf.inspect.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.MailRequest;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

import java.util.List;

@Setter
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = MailRequestWrapper.class)
public class MailRequestWrapper extends InstanceEventTrace implements Wrapper<MailRequest> {
    @Delegate
    @JsonIgnore
    private final MailRequest request = new MailRequest();

    @Deprecated(since = "v1.1")
    private List<MailRequestStageWrapper> actions;

    @Override
    public MailRequest unwrap() {
        return request;
    }
}
