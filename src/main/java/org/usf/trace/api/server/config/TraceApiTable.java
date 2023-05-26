package org.usf.trace.api.server.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.TableDecorator;

import java.util.function.Function;

@RequiredArgsConstructor
public enum TraceApiTable implements TableDecorator {

    AGREEMENTS("E_API_TRC", DataConstants::agrColumns),

    AGREEMENTS1("E_API_TRC",DataConstants::agrColumns1);
	
    @NonNull
    private final String tableName;
    @NonNull
    private final Function<TraceApiColumn, String> columnMap;

    @Override
    public String identity() {return name();}

    @Override
    public String reference() {return tableName;}

    @Override
    public String columnName(ColumnDecorator desc) {
        return columnMap.apply((TraceApiColumn) desc);
    }

    @Override
    public String sql() {
        return  tableName;
    }
}
