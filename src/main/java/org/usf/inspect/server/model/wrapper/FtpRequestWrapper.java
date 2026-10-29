package org.usf.inspect.server.model.wrapper;

import java.util.List;

import org.usf.inspect.EventTrace;
import org.usf.inspect.FtpRequestStage;
import org.usf.inspect.server.model.FtpRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = FtpRequestWrapper.class)
@Deprecated(since = "v1.1")
public class FtpRequestWrapper implements EventTrace {
    @Delegate
    @JsonIgnore
    private final FtpRequest request = new FtpRequest();

    private List<FtpRequestStage> actions;
}
