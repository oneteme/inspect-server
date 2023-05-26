package org.usf.trace.api.server.metadata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public final class CombinedFieldMetadata  implements FieldMetadata {
	
    private final String label;
    private final List<SimpleFieldMetadata> fields;

}
