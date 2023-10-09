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

    OUT("E_API_REQ", DataConstants::outReqColumns),
    REQUEST("E_API_SES", DataConstants::incReqColumns),
    SESSION("E_MAIN_SES", DataConstants::sessionColumns),
    QUERY("E_DB_REQ",DataConstants::outQryColumns),
    STAGES("E_STG",DataConstants::outStgColumns),
    DBACTION("E_DB_ACT",DataConstants::dbActColumns);

    @NonNull
    private final String tableName;
    @NonNull
    private final Function<TraceApiColumn, String> columnMap;

    @Override
    public String identity() {
        return name().toLowerCase();
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
