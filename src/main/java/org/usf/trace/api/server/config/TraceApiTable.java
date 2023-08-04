package org.usf.trace.api.server.config;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.function.Function;

import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiTable implements TableDecorator {

    INCOMING_REQUEST_TABLE("e_in_req", DataConstants::incReqColumns);

    @NonNull
    private final String tableName;
    @NonNull
    private final Function<TraceApiColumn, String> columnMap;

    @Override
    public String identity() {
        return name();
    }

    @Override
    public Optional<String> columnName(ColumnDecorator desc) {
        return ofNullable(columnMap.apply((TraceApiColumn) desc));
    }

	@Override
	public String tableName() {
		return tableName;
	}
}
