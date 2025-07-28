package org.usf.inspect.server.model.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.usf.inspect.core.LocalRequest;
import org.usf.inspect.server.model.InstanceEventTrace;
import org.usf.inspect.server.model.Wrapper;

import java.util.List;

@Getter
@Setter
public class LocalRequestWrapper extends InstanceEventTrace implements Wrapper<LocalRequest> {
    @Delegate
    @JsonIgnore
    private final LocalRequest request = new LocalRequest();

    private List<ExceptionInfoWrapper> exceptions;
    private boolean failed;

    @Override
    public LocalRequest unwrap() {
        return request;
    }

//    public ExceptionInfo getException(){
//        if(exceptions != null && !exceptions.isEmpty()){
//            return exceptions.getLast();
//        }
//        return getException();
//    }
}
