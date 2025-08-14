package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.Setter;
import org.usf.inspect.core.MainSession;

import java.util.List;

@Getter
@Setter
public class Analytic extends MainSession {
    private List<UserAction> userActions;
}
