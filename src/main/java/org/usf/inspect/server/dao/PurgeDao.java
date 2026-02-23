package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBOrder;
import org.usf.jquery.core.Operator;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.sql.Timestamp.from;
import static java.time.Duration.ofDays;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.core.SessionContextManager.emitError;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.INSTANCE;
import static org.usf.jquery.core.DBColumn.rank;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PurgeDao {

    private final ObjectMapper mapper;
    private final JdbcTemplate template;

    public List<InstanceEnvironment> getInstances() {
        return INSPECT.execute(v ->
                v.columns(
                        INSTANCE.column(TYPE),
                		INSTANCE.column(ENVIRONEMENT),
                        INSTANCE.column(APP_NAME),
                        INSTANCE.column(CONFIGURATION))
                .filters(rank().over(
                		new DBColumn[]{INSTANCE.column(ENVIRONEMENT), INSTANCE.column(APP_NAME), INSTANCE.column(TYPE)},
                        new DBOrder[] {INSTANCE.column(END).coalesce(Operator.ctimestamp().operation()).desc(), INSTANCE.column(START).desc()}).eq(1)), rs -> {
            List<InstanceEnvironment> environments = new ArrayList<>();
            while (rs.next()) {
                InspectCollectorConfiguration conf = null;
                try {
                    conf = rs.getString(CONFIGURATION.reference()) != null
                            ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class)
                            : null;
                } catch (JsonProcessingException e) {
                    emitError("Error parsing configuration for instance [" + rs.getString(ENVIRONEMENT.reference()) + "]:[" + rs.getString(APP_NAME.reference()) + "]");
                }
                finally {
                    if(conf == null) {
                        conf = new InspectCollectorConfiguration();
                        var rmt = new RestRemoteServerProperties();
                        rmt.setRetentionMaxAge(ofDays(60));
                        conf.getTracing().setRemote(rmt);
                    }
                }
                environments.add(new InstanceEnvironment(
                        null,
                        null,
                        InstanceType.valueOf(rs.getString(TYPE.reference())),
                        rs.getString(APP_NAME.reference()),
                        null,
                        rs.getString(ENVIRONEMENT.reference()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        conf
                ));
            }
            return environments;
        });
    }

    public int purgeRequest(String tableSuffix, String ids, Timestamp dateLimit, boolean withEnd) {
        return template.update("DELETE FROM e_" + tableSuffix +
                " WHERE dh_str < '" + dateLimit + "'" +
                (withEnd ? " AND dh_end < '" + dateLimit + "'" : "") +
                " AND cd_ins IN (" + ids + ");");
    }

    public int purgeRequest(String tableSuffix) {
        return template.update("DELETE FROM e_" + tableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_env_ins WHERE id_ins = cd_ins);");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeRequestStage(String stageTableSuffix, String tableSuffix, String type) {
        var queryStage = "DELETE FROM e_" + stageTableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_" + tableSuffix + " = cd_" + tableSuffix + ");";
        var queryException = "DELETE FROM e_exc_inf" +
                " WHERE va_typ = '" + type + "'" +
                " AND NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_" + tableSuffix + " = cd_rqt);";
        return stream(template.batchUpdate(queryStage, queryException)).sum();
    }

    public int purgeSessionStage(String stageTableSuffix, String tableSuffix) {
        return template.update("DELETE FROM e_" + stageTableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_ses = cd_prn_ses);");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeRequestStage(String tableSuffix, String stageTableSuffix, String type, String ids, Timestamp dateLimit) {
        var subQuery = "SELECT rqt.id_" + tableSuffix +
                " FROM e_" + tableSuffix + " rqt " +
                " WHERE rqt.cd_ins IN (" + ids + ") " +
                " AND rqt.dh_str < '" + dateLimit + "' " +
                " AND rqt.dh_end < '" + dateLimit + "'";

        var stageQuery = "DELETE FROM e_" + stageTableSuffix +
                 " WHERE cd_" + tableSuffix + " IN (" + subQuery + ") " +
                 " AND dh_str < '" + dateLimit + "' " +
                 " AND dh_end < '" + dateLimit + "'";

        var exceptionQuery = "DELETE FROM e_exc_inf" +
                    " WHERE cd_rqt IN (" + subQuery + ") " +
                    " AND va_typ = '" + type + "'";

        return stream(template.batchUpdate(stageQuery, exceptionQuery)).sum();
    }

    public int purgeSessionStage(String tableSuffix, String stageTableSuffix, String ids, Timestamp dateLimit) {
        var subQuery = "SELECT ses.id_ses" +
                " FROM e_" + tableSuffix + " ses " +
                " WHERE ses.cd_ins IN (" + ids + ") " +
                " AND ses.dh_str < '" + dateLimit + "' " +
                " AND ses.dh_end < '" + dateLimit + "'";

        return template.update("DELETE FROM e_" + stageTableSuffix +
                " WHERE cd_prn_ses IN (" + subQuery + ") " +
                " AND dh_str < '" + dateLimit + "' " +
                " AND dh_end < '" + dateLimit + "'");
    }

    public int purgeInstance(String env, String app, Timestamp dateLimit) {
        return template.update(format("DELETE FROM e_env_ins WHERE dh_end < '%s' AND va_env = '%s' AND va_app = '%s';", dateLimit, env, app));
    }

    public void vacuumAnalyze() {
    	Stream.of("e_rst_ses",
    			"e_main_ses",
    			"e_rst_rqt",
    			"e_dtb_rqt",
    			"e_ftp_rqt",
    			"e_smtp_rqt",
    			"e_ldap_rqt",
    			"e_lcl_rqt",
    			"e_rst_ses_stg",
    			"e_rst_rqt_stg",
    			"e_smtp_stg",
    			"e_ftp_stg",
    			"e_ldap_stg",
    			"e_dtb_stg",
    			"e_ins_trc",
    			"e_rsc_usg")
    	.map(v-> "VACUUM ANALYZE "+v+';')
    	.forEach(q->{
    		try {
    			template.execute(q); //H2 does not support vacuum analyze
    		}
    		catch (Exception e) {
    			log.error("Error during vacuum analyze on table {}: {}", q, e.getMessage());
			}
    	});
    }

    public List<String> selectInstanceIds(Timestamp dateLimit, String env, String app, InstanceType type) {
        var args = new ArrayList<>(3);
        args.add(dateLimit);
        args.add(type.name());
        return template.queryForList("SELECT id_ins FROM e_env_ins WHERE dh_str<? AND dh_end IS NULL AND va_typ = ?" +
                " AND va_env" + (nonNull(env) && args.add(env) ? "=?" : " IS NULL") +
                " AND va_app" + (nonNull(app) && args.add(app) ? "=?" : " IS NULL"), String.class, args.toArray());
    }
}
