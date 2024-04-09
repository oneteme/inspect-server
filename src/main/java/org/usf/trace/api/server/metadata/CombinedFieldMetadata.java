package org.usf.trace.api.server.metadata;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CombinedFieldMetadata  implements FieldMetadata {
	
    private final String label;
    private final List<SimpleFieldMetadata> fields;

}
