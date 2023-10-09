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

    OUT("e_api_req", DataConstants::outReqColumns),
    REQUEST("e_api_ses", DataConstants::incReqColumns),
    SESSION("e_main_ses", DataConstants::sessionColumns),
    QUERY("e_db_req",DataConstants::outQryColumns),
    STAGES("e_stg",DataConstants::outStgColumns),
    DBACTION("e_db_act",DataConstants::dbActColumns);

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
