package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.server.model.Wrapper;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = FtpRequestStageWrapper.class)
public class FtpRequestStageWrapper implements EventTrace, Wrapper<FtpRequestStage> {
    @Delegate
    @JsonIgnore
    private FtpRequestStage stage = new FtpRequestStage();

    @Override
    public FtpRequestStage unwrap() {
        return stage;
    }
}
