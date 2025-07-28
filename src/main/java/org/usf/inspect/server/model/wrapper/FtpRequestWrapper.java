package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.FtpRequest;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

import java.util.List;

@Getter
@Setter
public class FtpRequestWrapper extends InstanceEventTrace implements Wrapper<FtpRequest> {
    @Delegate
    @JsonIgnore
    private final FtpRequest request = new FtpRequest();

    @Deprecated(since = "v1.1", forRemoval = true)
    private List<FtpRequestStage> actions;

    @Override
    public FtpRequest unwrap() {
        return request;
    }
}
