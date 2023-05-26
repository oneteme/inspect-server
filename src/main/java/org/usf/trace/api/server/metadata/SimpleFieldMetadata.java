package org.usf.trace.api.server.metadata;

import org.usf.trace.api.server.config.TraceApiColumn;
import org.usf.trace.api.server.config.TraceApiTable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SimpleFieldMetadata implements FieldMetadata {

    private final TraceApiTable table;
    private final TraceApiColumn id;
    private final String reference;
    private final String label;
    private final String unit;

}
