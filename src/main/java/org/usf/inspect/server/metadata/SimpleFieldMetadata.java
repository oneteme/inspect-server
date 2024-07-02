package org.usf.inspect.server.metadata;

import org.usf.inspect.server.config.TraceApiColumn;
import org.usf.inspect.server.config.TraceApiTable;

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
