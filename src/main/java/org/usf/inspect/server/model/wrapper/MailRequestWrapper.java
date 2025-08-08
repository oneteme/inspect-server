package org.usf.inspect.server.model.wrapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.MailRequest;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.server.model.Wrapper;

import java.util.List;

@Setter
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = MailRequestWrapper.class)
@Deprecated(since = "v1.1")
public class MailRequestWrapper implements EventTrace, Wrapper<MailRequest> {
    @Delegate
    @JsonIgnore
    private final MailRequest request = new MailRequest();

    private List<MailRequestStage> actions;

    @Override
    public MailRequest unwrap() {
        return request;
    }
}
