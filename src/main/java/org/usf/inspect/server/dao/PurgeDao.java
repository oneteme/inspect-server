package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.InspectApplication;
import org.usf.inspect.server.erm.InspectStore;
import org.usf.inspect.server.erm.InstanceCatalog;
import org.usf.inspect.core.TraceType;
import org.usf.jquery.core.Column;
import org.usf.jquery.core.Order;
import org.usf.jquery.mvc.StoreManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.sql.Types.OTHER;
import static java.time.Duration.ofDays;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.SessionMask.*;
import static org.usf.inspect.core.SessionContextManager.emitError;
import static org.usf.inspect.server.JsonUtils.fromJson;
import static org.usf.inspect.server.JsonUtils.toJson;
import static org.usf.jquery.core.Column.ctimestamp;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.usf.jquery.core.QueryComposer;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PurgeDao {

    private static final Duration DEFAULT_RETENTION = ofDays(30);
   // InspectApplication app = InspectApplication.defaultInstance;

    private static final Retention DEFAULT_RETENTION_CONFIG = new Retention(DEFAULT_RETENTION, DEFAULT_RETENTION);
    private final NamedParameterJdbcTemplate namedTemplate;

    private final ObjectMapper mapper;
    private final JdbcTemplate template;
    public record PurgeScope(
            InstanceType type,
            String app,
            String namespace,
            Retention retention
    ) {}



    public List<PurgeScope> selectInstances() {
        InspectStore store = StoreManager.getInstance()
                .getStore(InspectStore.class);

        InstanceCatalog instance = store.instance();

        var query = new QueryComposer()
                .columns(
                        instance.type(),
                        instance.appName(),
                        instance.namespace(),
                        instance.configuration()
                )
                .criterias(
                        Column.rank().over(
                                new Column[]{
                                        instance.namespace(),
                                        instance.appName(),
                                        instance.type()
                                },
                                new Order[]{
                                        instance.start().desc()
                                }
                        ).eq(1)
                );

        return store.execute(
                query.compose(store),
                rs -> mapScopes(rs, instance)
        );
    }

    public List<UUID> selectInstanceIds(Timestamp before, String nsp, String app, InstanceType type) {
        var args = new ArrayList<>(3);
        args.add(before);
        args.add(type.name());
        return template.queryForList("SELECT id_ins FROM e_env_ins WHERE dh_str<? AND dh_end IS NULL AND va_typ = ?" +
                " AND cd_nsp" + (nonNull(nsp) && args.add(nsp) ? "=?" : " IS NULL") +
                " AND va_app" + (nonNull(app) && args.add(app) ? "=?" : " IS NULL"), UUID.class, args.toArray());
    }


    public int purgeAbandonedInstances(String namespace, String app, Timestamp dateLimit) {
        return template.update("""
        DELETE FROM e_env_ins
        WHERE id_ins IN (
            SELECT i.id_ins
            FROM e_env_ins i
            LEFT JOIN e_ins_trc t ON t.cd_ins = i.id_ins
            WHERE i.dh_end IS NULL
              AND i.va_app = ?
              AND i.cd_nsp = ?
            GROUP BY i.id_ins, i.dh_str
            HAVING COALESCE(MAX(t.dh_str), i.dh_str) < ?
        )
        """, app, namespace, dateLimit);
    }


    public int purgeInstance(String env, String app, Timestamp dateLimit) {
        return template.update("DELETE FROM e_env_ins WHERE dh_end < '" + dateLimit + "' AND va_env = '" + env + "' AND va_app = '" + app + "';");
    }

    public int purgeInstanceTrace(List<UUID>  ids, Timestamp before){
        return purgeRequest("ins_trc", ids, before, false);
    }

    public int purgeInstanceTrace(LocalDate before){
        return purgeRequest("ins_trc",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeResourceUsage(List<UUID>  ids, Timestamp before){
        return purgeRequest("rsc_usg", ids, before, false);
    }

    public int purgeResourceUsage(LocalDate before){
        return purgeRequest("rsc_usg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeMainSession(List<UUID> ids, Timestamp before){
        return purgeRequest("main_ses", ids, before, true);
    }

    public int purgeMainSession(LocalDate before){
        return purgeRequest("main_ses",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeMainSessionStage(LocalDate before){
        return purgeSessionStage("main_ses", "ses_evt",Timestamp.valueOf(before.atStartOfDay()));//TODO CHECK ses_evt
    }

    public int purgeRestSession(List<UUID>  ids, Timestamp before){
        return purgeRequest("rst_ses", ids, before, true);
    }

    public int purgeRestSession(LocalDate before){
        return purgeRequest("rst_ses",Timestamp.valueOf(before.atStartOfDay()));
    }



    public int purgeRestSessionStage(LocalDate before) {
        return purgeSessionStage("rst_ses", "rst_ses_stg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeRestRequest(List<UUID>  ids, Timestamp before){
        return purgeRequest("rst_rqt", ids, before, true);
    }

    public int purgeRestRequest(LocalDate before){
        return purgeRequest("rst_rqt",Timestamp.valueOf(before.atStartOfDay()));
    }


    public int purgeRestRequestStage(LocalDate before){
        return purgeRequestStage("rst_rqt", "rst_rqt_stg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeMailRequestStage(LocalDate before){
        return purgeRequestStage("smtp_rqt", "smtp_mail",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeSmtpRequest(List<UUID>  ids, Timestamp before){
        return purgeRequest("smtp_rqt", ids, before, true);
    }

    public int purgeSmtpRequest(LocalDate before){
        return purgeRequest("smtp_rqt",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeSmtpRequestStage(LocalDate before){
        return purgeRequestStage("smtp_rqt", "smtp_stg",Timestamp.valueOf(before.atStartOfDay()));
    }


    public int purgeFtpRequest(List<UUID>  ids, Timestamp before){
        return purgeRequest("ftp_rqt", ids, before, true);
    }

    public int purgeFtpRequest(LocalDate before){
        return purgeRequest("ftp_rqt", Timestamp.valueOf(before.atStartOfDay()));
    }


    public int purgeFtpRequestStage(LocalDate before){
        return purgeRequestStage("ftp_rqt", "ftp_stg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeLdapRequest(List<UUID>  ids, Timestamp before){
        return purgeRequest("ldap_rqt", ids, before, true);
    }

    public int purgeLdapRequest(LocalDate before ){
        return purgeRequest("ldap_rqt",Timestamp.valueOf(before.atStartOfDay()));
    }


    public int purgeLdapRequestStage(LocalDate before){
        return purgeRequestStage("ldap_rqt", "ldap_stg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeDtbRequest(List<UUID>  ids, Timestamp dateLimit){
        return purgeRequest("dtb_rqt", ids, dateLimit, true);
    }

    public int purgeDtbRequest(LocalDate before){
        return purgeRequest("dtb_rqt",Timestamp.valueOf(before.atStartOfDay()));
    }


    public int purgeDtbRequestStage(LocalDate before){
        return purgeRequestStage("dtb_rqt", "dtb_stg",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeLocalRequest(List<UUID>  ids, Timestamp before){
        return purgeRequest("lcl_rqt", ids, before, true);
    }

    public int purgeLocalRequest(LocalDate before){
        return purgeRequest("lcl_rqt",Timestamp.valueOf(before.atStartOfDay()));
    }

    public int purgeLogEntry(List<UUID>  ids, Timestamp before){
        return purgeRequest("log_ent", ids, before, false);
    }

    public int purgeLogEntry(LocalDate before){
        return purgeRequest("log_ent",Timestamp.valueOf(before.atStartOfDay()));
    }




    public Stream<Runnable> vacuumTables(){
        return Stream.of(
                //instance !?
                ()-> vacuum("e_rst_ses"),
                ()-> vacuum("e_main_ses"),
                ()-> vacuum("e_rst_rqt"),
                ()-> vacuum("e_dtb_rqt"),
                ()-> vacuum("e_ftp_rqt"),
                ()-> vacuum("e_smtp_rqt"),
                ()-> vacuum("e_ldap_rqt"),
                ()-> vacuum("e_lcl_rqt"),
                ()-> vacuum("e_rst_ses_stg"),
                ()-> vacuum("e_rst_rqt_stg"),
                ()-> vacuum("e_smtp_stg"),
                ()-> vacuum("e_ftp_stg"),
                ()-> vacuum("e_ldap_stg"),
                ()-> vacuum("e_dtb_stg"),
                ()-> vacuum("e_ins_trc"),
                ()-> vacuum("e_rsc_usg"));
    }

    private int purgeRequest(String tableSuffix, List<UUID> ids, Timestamp before, boolean withEnd)
    {
        if (ids.isEmpty()) {
            return 0;
        }

        String sql = "DELETE FROM e_" + tableSuffix +
                " WHERE dh_str < :before" +
                (withEnd ? " AND dh_end < :before" : "") +
                " AND cd_ins IN (:ids)";

        var params = new MapSqlParameterSource()
                .addValue("before", before)
                .addValue("ids", ids);

        return namedTemplate.update(sql, params);
    }



    private int purgeRequest(String tableSuffix, Timestamp before) {
        return template.update(
                "DELETE FROM e_" + tableSuffix +
                        " WHERE dh_str < '" + before + "'" +
                        " AND NOT EXISTS (" +
                        "SELECT 1 FROM e_env_ins WHERE id_ins = cd_ins" +
                        ");"
        );
    }


    public  int purgeBrowserConfig() {
        return template.update("DELETE FROM o_brw_cfg" +
                " WHERE  NOT EXISTS (SELECT 1 FROM e_env_ins WHERE cd_prn_ses = id_ins);");
    }

    private int purgeRequestStage(String tableSuffix, String stageTableSuffix, Timestamp before) {
        var queryStage = "DELETE FROM e_" + stageTableSuffix +
                " WHERE dh_str < '" + before + "'" +
                " AND NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_" + tableSuffix + " = cd_" + tableSuffix + ")";
        return stream(template.batchUpdate(queryStage)).sum();
    }

    public int purgeException() {
      return   stream(template.batchUpdate(Arrays.stream(TraceType.values()).map(t-> purgeBuildException(t) ).toArray(String[]::new))).sum();

    }

    private String purgeBuildException(TraceType trcType) {
        var tableSuffix = switch (trcType) {
            case MAIN_SES -> "main_ses";
            case HTTP_SES -> "rst_ses";
            case FTP_REQ -> "ftp_rqt";
            case JDBC_REQ -> "dtb_rqt";
            case LDAP_REQ -> "ldap_rqt";
            case LCL_REQ -> "lcl_rqt";
            case SMTP_REQ -> "smtp_rqt";
            case HTTP_REQ -> "rst_rqt";
        };

        var idSuffix = tableSuffix;
        if (trcType == TraceType.MAIN_SES || trcType == TraceType.HTTP_SES) {
            idSuffix = "ses";
        }
        return "DELETE FROM e_exc_inf" +
                " WHERE va_trc_typ='" + trcType.getValue() + "'" +
        " AND  NOT EXISTS (SELECT 1 FROM e_"
                + tableSuffix + " WHERE  id_" + idSuffix + " = cd_rqt)";
    }


    private int purgeSessionStage(String tableSuffix, String stageTableSuffix, Timestamp before) {
        return template.update("DELETE FROM e_" + stageTableSuffix +
                " WHERE dh_str< '" + before + "'" +" AND NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_ses = cd_prn_ses);");
    }
    public int purgeSessionEvent(LocalDate now) {
        var before=Timestamp.valueOf(now.atStartOfDay());
        return template.update("DELETE FROM e_ses_evt"  +
                " WHERE dh_str < '" + before + "'" + " AND NOT EXISTS (SELECT 1 FROM e_rst_ses  WHERE id_ses = cd_prn_ses) AND " +
                "NOT EXISTS (SELECT 1 FROM e_main_ses WHERE id_ses = cd_prn_ses);");
    }

    private int purgeRequestStage(String tableSuffix, String stageTableSuffix, List<UUID> ids, Timestamp before) {
        var subQuery = "SELECT rqt.id_" + tableSuffix +
                " FROM e_" + tableSuffix + " rqt " +
                " WHERE rqt.cd_ins IN (" + ids + ") " +
                " AND rqt.dh_str < '" + before + "' " +
                " AND rqt.dh_end < '" + before + "'";

        var stageQuery = "DELETE FROM e_" + stageTableSuffix +
                " WHERE cd_" + tableSuffix + " IN (" + subQuery + ") " +
                " AND dh_str < '" + before + "' " +
                " AND dh_end < '" + before + "'";

        var exceptionQuery = "DELETE FROM e_exc_inf" +
                " WHERE cd_rqt IN (" + subQuery + ") " ;
              //  " AND va_typ = '" + type + "'";

        return stream(template.batchUpdate(stageQuery, exceptionQuery)).sum();
    }

    private void vacuum(String tableSuffix) {
        try {
            template.execute("VACUUM ANALYZE " + tableSuffix + ";");
        }
        catch (Exception e) {
            log.error("Error during vacuum analyze on table {}: {}", tableSuffix, e.getMessage());
        }
    }

    List<PurgeScope> mapScopes(ResultSet rs, InstanceCatalog instance) throws SQLException {
        var out = new ArrayList<PurgeScope>();
        while (rs.next()) {
            var app = rs.getString(instance.appName().toString());
            var namespace = rs.getString(instance.namespace().toString());
            var type = InstanceType.valueOf(rs.getString(instance.type().toString()));
            var raw = rs.getString(instance.configuration().toString());

            var config = fromJson(raw, InspectCollectorConfiguration.class);
            var rtt = config == null ? DEFAULT_RETENTION_CONFIG : config.getTracing().getRemote().getRetentionMaxAge();
            // Extract retention durations directly from the Retention object
            out.add(new PurgeScope(type, app, namespace, rtt));
        }
        return out;
    }




}