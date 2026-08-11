package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stores a pair of related values.
 */
@Getter
@RequiredArgsConstructor
public class Pair<R,C>{
    private final R v1;
    private final C v2;
}
