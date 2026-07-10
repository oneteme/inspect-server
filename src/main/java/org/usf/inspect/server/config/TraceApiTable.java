package org.usf.inspect.server.config;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiTable {

//    REST_REQUEST(ColumnConstant::restRequestColumns, JoinConstant::restRequestJoins),
//    REST_REQUEST_STAGE(ColumnConstant::restRequestStageColumns, JoinConstant::restRequestStageJoins),
//	REST_SESSION(ColumnConstant::restSessionColumns, JoinConstant::restSessionJoins),
//	REST_SESSION_STAGE(ColumnConstant::restSessionStageColumns),
//    MAIN_SESSION(ColumnConstant::mainSessionColumns, JoinConstant::mainSessionJoins),
//    DATABASE_REQUEST(ColumnConstant::databaseRequestColumns, JoinConstant::databaseRequestJoins),
//    DATABASE_STAGE(ColumnConstant::databaseStageColumns, JoinConstant::databaseStageJoins),
//	FTP_REQUEST(ColumnConstant::ftpRequestColumns, JoinConstant::ftpRequestJoins),
//	FTP_STAGE(ColumnConstant::ftpStageColumns, JoinConstant::ftpStageJoins),
//	SMTP_REQUEST(ColumnConstant::smtpRequestColumns, JoinConstant::smtpRequestJoins),
//	SMTP_STAGE(ColumnConstant::smtpStageColumns, JoinConstant::smtpStageJoins),
//	SMTP_MAIL(ColumnConstant::smtpMailColumns),
//	LDAP_REQUEST(ColumnConstant::ldapRequestColumns, JoinConstant::ldapRequestJoins),
//	LDAP_STAGE(ColumnConstant::ldapStageColumns, JoinConstant::ldapStageJoins),
//    LOCAL_REQUEST(ColumnConstant::localRequestColumns, JoinConstant::localRequestJoins),
//    EXCEPTION(ColumnConstant::exceptionColumns, JoinConstant::exceptionJoins),
//    INSTANCE(ColumnConstant::instanceColumns, JoinConstant::instanceJoins),
//	USER_ACTION(ColumnConstant::userActionColumns, JoinConstant::userActionJoins),
//	INSTANCE_TRACE(ColumnConstant::instanceTraceColumns),
//	LOG_ENTRY(ColumnConstant::logEntryColumns),
//	RESOURCE_USAGE(ColumnConstant::resourceUsageColumns);
//
//	@NonNull
//    private final Function<TraceApiColumn, String> columnMap;
//	private final Function<String, Builder<ViewDecorator, ViewJoin[]>> builder;
//
//	TraceApiTable(@NonNull Function<TraceApiColumn, String> columnMap) {
//		this.columnMap = columnMap;
//		this.builder = null;
//	}
//
//	@Override
//    public String identity() {
//        return name().toLowerCase();
//    }
//
//    @Override
//    public String columnName(ColumnDecorator desc) { //nullable
//        return columnMap.apply((TraceApiColumn) desc);
//    }
//
//	@Override
//	public Builder<ViewDecorator, ViewJoin[]> joinBuilder(String name) {
//		return builder == null ? null : builder.apply(name);
//	}
}
