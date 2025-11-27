package org.usf.inspect.server.dto;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.UserAction;

import java.util.List;

@Getter
@Setter
public class AnalyticDto extends MainSession {
    private List<UserAction> userActions;
}
