package org.usf.inspect.server.model.object;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public final class Mail {
    private String subject;
    private String contentType;
    private String[] from;
    private String[] recipients;
    private String[] replyTo;
    private int size;

    private long id;
}
