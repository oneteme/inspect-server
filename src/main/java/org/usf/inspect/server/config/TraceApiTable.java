package org.usf.inspect.server.config;

import static java.lang.Integer.parseInt;
import static org.usf.inspect.server.config.TraceApiColumn.HOST;
import static org.usf.inspect.server.config.TraceApiColumn.ID;
import static org.usf.inspect.server.config.TraceApiColumn.PARENT;
import static org.usf.inspect.server.config.TraceApiColumn.PORT;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.jquery.core.DBColumn.allColumns;
import static org.usf.jquery.core.ViewJoin.innerJoin;

import java.util.function.Function;

import org.usf.inspect.server.config.constant.ColumnConstant;
import org.usf.inspect.server.config.constant.JoinConstant;
import org.usf.jquery.core.*;
import org.usf.jquery.web.Builder;
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.ViewBuilder;
import org.usf.jquery.web.ViewDecorator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiTable implements ViewDecorator {

    REST_REQUEST(ColumnConstant::restRequestColumns, JoinConstant::restRequestJoins),
    REST_SESSION(ColumnConstant::restSessionColumns, JoinConstant::restSessionJoins),
//  REST_SESSION2(DataConstants::restSessionColumns),
    MAIN_SESSION(ColumnConstant::mainSessionColumns, JoinConstant::mainSessionJoins),
    DATABASE_REQUEST(ColumnConstant::databaseRequestColumns, JoinConstant::databaseRequestJoins),
    DATABASE_STAGE(ColumnConstant::databaseStageColumns, JoinConstant::databaseStageJoins),
	FTP_REQUEST(ColumnConstant::ftpRequestColumns, JoinConstant::ftpRequestJoins),
	FTP_STAGE(ColumnConstant::ftpStageColumns, JoinConstant::ftpStageJoins),
	SMTP_REQUEST(ColumnConstant::smtpRequestColumns, JoinConstant::smtpRequestJoins),
	SMTP_STAGE(ColumnConstant::smtpStageColumns, JoinConstant::smtpStageJoins),
	SMTP_MAIL(ColumnConstant::smtpMailColumns),
	LDAP_REQUEST(ColumnConstant::ldapRequestColumns, JoinConstant::ldapRequestJoins),
	LDAP_STAGE(ColumnConstant::ldapStageColumns, JoinConstant::ldapStageJoins),
    LOCAL_REQUEST(ColumnConstant::localRequestColumns, JoinConstant::localRequestJoins),
    EXCEPTION(ColumnConstant::exceptionColumns, JoinConstant::exceptionJoins),
    INSTANCE(ColumnConstant::instanceColumns, JoinConstant::instanceJoins),
	USER_ACTION(ColumnConstant::userActionColumns, JoinConstant::userActionJoins);

    @NonNull
    private final Function<TraceApiColumn, String> columnMap;
	private final Function<String, Builder<ViewDecorator, ViewJoin[]>> builder;

	TraceApiTable(@NonNull Function<TraceApiColumn, String> columnMap) {
		this.columnMap = columnMap;
		this.builder = null;
	}

	@Override
    public String identity() {
        return name().toLowerCase();
    }

    @Override
    public String columnName(ColumnDecorator desc) { //nullable
        return columnMap.apply((TraceApiColumn) desc);
    }

	@Override
	public Builder<ViewDecorator, ViewJoin[]> joinBuilder(String name) {
		return builder == null ? null : builder.apply(name);
	}
}
