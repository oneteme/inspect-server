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

    REST_REQUEST("e_rst_rqt", DataConstants::restRequestColumns),
    REST_SESSION("e_rst_ses", DataConstants::restSessionColumns),
    REST_SESSION2("e_rst_ses", DataConstants::restSessionColumns),
    MAIN_SESSION("e_main_ses", DataConstants::mainSessionColumns),
    DATABASE_REQUEST("e_dtb_rqt",DataConstants::databaseRequestColumns),
    DATABASE_STAGE("e_dtb_stg",DataConstants::databaseStageColumns),
    FTP_REQUEST("e_ftp_rqt",DataConstants::ftpRequestColumns),
    FTP_STAGE("e_ftp_stg",DataConstants::ftpStageColumns),

    LOCAL_REQUEST("e_lcl_rqt",DataConstants::localRequestColumns),
    INSTANCE("e_env_ins", DataConstants::instanceColumns);

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
