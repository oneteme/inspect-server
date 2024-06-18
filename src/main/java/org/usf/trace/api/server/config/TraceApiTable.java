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

    APIREQUEST("e_api_req", DataConstants::outReqColumns),
    APISESSION("e_api_ses", DataConstants::incReqColumns),
    APISESSION2("e_api_ses", DataConstants::incReqColumns),
    MAINSESSION("e_main_ses", DataConstants::sessionColumns),
    DBQUERY("e_db_req",DataConstants::outQryColumns),
    STAGES("e_stg",DataConstants::outStgColumns),
    DBACTION("e_db_act",DataConstants::dbActColumns),
    INSTANCE("e_ins_env", DataConstants::instanceEnvColumns);

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
