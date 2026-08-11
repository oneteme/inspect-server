package org.usf.inspect.server.metadata;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes metadata composed of multiple simple fields grouped under one label.
 */
@Getter
@RequiredArgsConstructor
public final class CombinedFieldMetadata  implements FieldMetadata {
	
    private final String label;
    private final List<SimpleFieldMetadata> fields;

}
