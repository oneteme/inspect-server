package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.InspectCollectorConfiguration;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.server.model.RequestCompletableType;
import org.usf.jquery.core.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.*;
import static java.sql.Timestamp.*;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.*;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.server.config.TraceApiColumn.*;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.inspect.server.config.TraceApiTable.*;
import static org.usf.jquery.core.DBColumn.rank;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PurgeDao {

    private static final List<String> TABLES_WITH_INSTANCE = List.of(
            "e_rst_ses", "e_main_ses", "e_rst_rqt", "e_dtb_rqt", "e_ftp_rqt", "e_smtp_rqt", "e_ldap_rqt", "e_lcl_rqt"
    );
    private static final List<String> TABLES_WITH_INSTANCE_WITHOUT_END = List.of(
            "e_ins_trc", "e_log_ent", "e_rsc_usg"
    );

    private static final String DELETE_BY_INSTANCE = "DELETE FROM %s WHERE dh_str < '%s' AND dh_end < '%s' AND cd_ins IN (%s);";
    private static final String DELETE_BY_INSTANCE_WITHOUT_END = "DELETE FROM %s WHERE dh_str < '%s' AND cd_ins IN (%s);";
    private static final String DELETE_BY_NO_INSTANCE = "DELETE FROM %s WHERE NOT EXISTS (SELECT 1 FROM %s WHERE %s = %s);";
    private static final String DELETE_EXCEPTION_BY_NO_INSTANCE = "DELETE FROM e_exc_inf WHERE va_typ = '%s' AND NOT EXISTS (SELECT 1 FROM %s WHERE %s = cd_rqt);";

    private final ObjectMapper mapper;
    private final JdbcTemplate template;

    public List<InstanceEnvironment> getInstances(String env) {
        return INSPECT.execute(v ->
                v.columns(
                		INSTANCE.column(ENVIRONEMENT),
                        INSTANCE.column(APP_NAME),
                        INSTANCE.column(CONFIGURATION))
                .filters(rank().over(
                		new DBColumn[]{INSTANCE.column(ENVIRONEMENT), INSTANCE.column(APP_NAME)},
                        new DBOrder[] {INSTANCE.column(START).desc()}).eq(1))
                .subViewQuery(INSTANCE.view(), sq -> {
                	sq.filters(INSTANCE.column(END).isNull());
                    if (nonNull(env)) {
                    	sq.filters(INSTANCE.column(ENVIRONEMENT).eq(env));
                    }}), rs -> {
            List<InstanceEnvironment> environments = new ArrayList<>();
            while (rs.next()) {
                try {
                    environments.add(new InstanceEnvironment(
                            null,
                            null,
                            null,
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
                            rs.getString(CONFIGURATION.reference()) != null ? mapper.readValue(rs.getString(CONFIGURATION.reference()), InspectCollectorConfiguration.class) : null
                    ));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return environments;
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public int[] purgeByInstance(Instant dateLimit, String env, String app) {
        List<String> sql = new ArrayList<>();
        var ids = selectInstanceIds(dateLimit, env, app);
        if (!ids.isEmpty()) {
            for (String table : TABLES_WITH_INSTANCE) {
                sql.add(format(DELETE_BY_INSTANCE, table, from(dateLimit), from(dateLimit), ids.stream().map(id -> "'" + id + "'").collect(joining(","))));
            }
            for (String table : TABLES_WITH_INSTANCE_WITHOUT_END) {
                sql.add(format(DELETE_BY_INSTANCE_WITHOUT_END, table, from(dateLimit), ids.stream().map(id -> "'" + id + "'").collect(joining(","))));
            }
        }
        sql.add(format("DELETE FROM e_env_ins WHERE dh_end < '%s' AND va_env %s AND va_app %s;", from(dateLimit), env != null ? " = '" + env + "'" : "IS NULL", app != null ? " = '" + app + "'" : "IS NULL"));
        return template.batchUpdate(sql.toArray(new String[0]));
    }

    @Transactional(rollbackFor = Throwable.class)
    public int[] finalizePurge() {
        return template.batchUpdate(
                format(DELETE_BY_NO_INSTANCE, "e_rst_ses", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_main_ses", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_rst_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_dtb_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_ftp_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_smtp_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_ldap_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_lcl_rqt", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_rst_ses_stg", "e_rst_ses", "id_ses", "cd_prn_ses"),
                format(DELETE_BY_NO_INSTANCE, "e_rst_rqt_stg", "e_rst_rqt", "id_rst_rqt", "cd_rst_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_smtp_stg", "e_smtp_rqt", "id_smtp_rqt", "cd_smtp_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_smtp_mail", "e_smtp_rqt", "id_smtp_rqt", "cd_smtp_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_ftp_stg", "e_ftp_rqt", "id_ftp_rqt", "cd_ftp_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_ldap_stg", "e_ldap_rqt", "id_ldap_rqt", "cd_ldap_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_dtb_stg", "e_dtb_rqt", "id_dtb_rqt", "cd_dtb_rqt"),
                format(DELETE_BY_NO_INSTANCE, "e_usr_acn", "e_main_ses", "id_ses", "cd_prn_ses"),
                format(DELETE_BY_NO_INSTANCE, "e_ins_trc", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_log_ent", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_BY_NO_INSTANCE, "e_rsc_usg", "e_env_ins", "id_ins", "cd_ins"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, REST, "e_rst_rqt", "id_rst_rqt"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, JDBC, "e_dtb_rqt", "id_dtb_rqt"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, LDAP, "e_ldap_rqt", "id_ldap_rqt"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, SMTP, "e_smtp_rqt", "id_smtp_rqt"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, FTP, "e_ftp_rqt", "id_ftp_rqt"),
                format(DELETE_EXCEPTION_BY_NO_INSTANCE, LOCAL, "e_lcl_rqt", "id_lcl_rqt")
        );
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
    	.forEach(template::execute);
    }

    private List<String> selectInstanceIds(Instant dateLimit, String env, String app) {
        var args = new ArrayList<>(3);
        args.add(from(dateLimit));
        return template.queryForList(new StringBuilder("SELECT id_ins FROM e_env_ins WHERE dh_str<? AND dh_end IS NULL")
        		.append(" AND va_env").append(nonNull(env) && args.add(env) ? "=?" : " IS NULL")
        		.append(" AND va_app").append(nonNull(app) && args.add(app) ? "=?" : " IS NULL")
        		.toString(), String.class, args.toArray());
    }
}
