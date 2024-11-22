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
import org.usf.jquery.web.ColumnDecorator;
import org.usf.jquery.web.CriteriaBuilder;
import org.usf.jquery.web.JoinBuilder;
import org.usf.jquery.web.PartitionBuilder;
import org.usf.jquery.web.ViewBuilder;
import org.usf.jquery.web.ViewDecorator;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TraceApiTable implements ViewDecorator {

    REST_REQUEST(ColumnConstant::restRequestColumns, JoinConstant::restRequestJoins),
    REST_SESSION(ColumnConstant::restSessionColumns, JoinConstant::restSessionJoins),

    TEST(ColumnConstant::restSessionColumns){
    	@Override
    	public ViewBuilder builder() {
    		return ()->
    			new QueryBuilder().columns(allColumns(REST_SESSION.view())).asView();
    	}

    	@Override
    	public CriteriaBuilder<DBFilter> criteria(String name) {
    		if("crt".equals(name)) {
    			return v-> column(PORT).gt(parseInt(v[0])).or(column(HOST).like("usf"));
    		}
    		return null;
    	}

    	@Override
    	public JoinBuilder join(String name) {
    		if("jnr".equals(name)) {
    			return ()-> new ViewJoin[] { innerJoin(REST_REQUEST.view(), column(ID).eq(REST_REQUEST.column(PARENT))) };
    		}
    		return null;
    	}

    	@Override
    	public PartitionBuilder partition(String name) {
    		if("par".equals(name)) {
    			return ()-> new Partition(new DBColumn[] {}, new DBOrder[] {column(START).order()});
    		}
    		return null;
    	}
    },
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
    INSTANCE(ColumnConstant::instanceColumns);

    @NonNull
    private final Function<TraceApiColumn, String> columnMap;
	private final Function<String, JoinBuilder> builder;

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
	public JoinBuilder join(String name) {
		return builder == null ? null : builder.apply(name);
	}
}
