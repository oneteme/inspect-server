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
import org.usf.inspect.core.InstanceType;
import org.usf.inspect.core.RestRemoteServerProperties;
import org.usf.jquery.core.DBColumn;
import org.usf.jquery.core.DBOrder;
import org.usf.jquery.core.Operator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.time.Duration.ofDays;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
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

    private static InspectCollectorConfiguration defaultConfig(InspectCollectorConfiguration conf) {
        if(conf == null) {
            conf = new InspectCollectorConfiguration();
            var rmt = new RestRemoteServerProperties();
            rmt.setRetentionMaxAge(ofDays(60));
            conf.getTracing().setRemote(rmt);
        }
        return conf;
    }

    private static InstanceEnvironment setInstanceEnvironment(String type, String env, String app, InspectCollectorConfiguration conf) {
        return new InstanceEnvironment(null, null, InstanceType.valueOf(type), app, null, env, null, null, null, null, null, null, null, null, conf);
    }

    public List<InstanceEnvironment> selectInstances() {
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
                    conf = defaultConfig(conf);
                }
                environments.add(setInstanceEnvironment(
                        rs.getString(TYPE.reference()),
                        rs.getString(APP_NAME.reference()),
                        rs.getString(ENVIRONEMENT.reference()),
                        conf));
            }
            return environments;
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

    public int purgeInstance(String env, String app, Timestamp dateLimit) {
        return template.update("DELETE FROM e_env_ins WHERE dh_end < '" + dateLimit + "' AND va_env = '" + env + "' AND va_app = '" + app + "';");
    }

    public int purgeInstanceTrace(String ids, Timestamp dateLimit){
        return purgeRequest("ins_trc", ids, dateLimit, false);
    }

    public int purgeInstanceTrace(){
        return purgeRequest("ins_trc");
    }

    public int purgeResourceUsage(String ids, Timestamp dateLimit){
        return purgeRequest("rsc_usg", ids, dateLimit, false);
    }

    public int purgeResourceUsage(){
        return purgeRequest("rsc_usg");
    }

    public int purgeMainSession(String ids, Timestamp dateLimit){
        return purgeRequest("main_ses", ids, dateLimit, true);
    }

    public int purgeMainSession(){
        return purgeRequest("main_ses");
    }

    public int purgeMainSessionStage(){
        return purgeSessionStage("main_ses", "usr_acn");
    }

    public int purgeRestSession(String ids, Timestamp dateLimit){
        return purgeRequest("rst_ses", ids, dateLimit, true);
    }

    public int purgeRestSession(){
        return purgeRequest("rst_ses");
    }

    public int purgeRestSessionStage(String ids, Timestamp dateLimit) {
        var subQuery = "SELECT ses.id_ses" +
                " FROM e_rst_ses ses " +
                " WHERE ses.cd_ins IN (" + ids + ") " +
                " AND ses.dh_str < '" + dateLimit + "' " +
                " AND ses.dh_end < '" + dateLimit + "'";

        return template.update("DELETE FROM e_rst_ses_stg" +
                " WHERE cd_prn_ses IN (" + subQuery + ") " +
                " AND dh_str < '" + dateLimit + "' " +
                " AND dh_end < '" + dateLimit + "'");
    }

    public int purgeRestSessionStage() {
        return purgeSessionStage("rst_ses", "rst_ses_stg");
    }

    public int purgeRestRequest(String ids, Timestamp dateLimit){
        return purgeRequest("rst_rqt", ids, dateLimit, true);
    }

    public int purgeRestRequest(){
        return purgeRequest("rst_rqt");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeRestRequestStage(String ids, Timestamp dateLimit){
        return purgeRequestStage("rst_rqt", "rst_rqt_stg", REST.name(), ids, dateLimit);
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeRestRequestStage(){
        return purgeRequestStage("rst_rqt", "rst_rqt_stg", REST.name());
    }

    public int purgeSmtpRequest(String ids, Timestamp dateLimit){
        return purgeRequest("smtp_rqt", ids, dateLimit, true);
    }

    public int purgeSmtpRequest(){
        return purgeRequest("smtp_rqt");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeSmtpRequestStage(){
        return purgeRequestStage("smtp_rqt", "smtp_stg", SMTP.name());
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeSmtpRequestStage(String ids, Timestamp dateLimit){
        return purgeRequestStage("smtp_rqt", "smtp_stg", SMTP.name(), ids, dateLimit);
    }

    public int purgeFtpRequest(String ids, Timestamp dateLimit){
        return purgeRequest("ftp_rqt", ids, dateLimit, true);
    }

    public int purgeFtpRequest(){
        return purgeRequest("ftp_rqt");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeFtpRequestStage(String ids, Timestamp dateLimit){
        return purgeRequestStage("ftp_rqt", "ftp_stg", FTP.name(), ids, dateLimit);
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeFtpRequestStage(){
        return purgeRequestStage("ftp_rqt", "ftp_stg", FTP.name());
    }

    public int purgeLdapRequest(String ids, Timestamp dateLimit){
        return purgeRequest("ldap_rqt", ids, dateLimit, true);
    }

    public int purgeLdapRequest(){
        return purgeRequest("ldap_rqt");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeLdapRequestStage(String ids, Timestamp dateLimit){
        return purgeRequestStage("ldap_rqt", "ldap_stg", LDAP.name(), ids, dateLimit);
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeLdapRequestStage(){
        return purgeRequestStage("ldap_rqt", "ldap_stg", LDAP.name());
    }

    public int purgeDtbRequest(String ids, Timestamp dateLimit){
        return purgeRequest("dtb_rqt", ids, dateLimit, true);
    }

    public int purgeDtbRequest(){
        return purgeRequest("dtb_rqt");
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeDtbRequestStage(String ids, Timestamp dateLimit){
        return purgeRequestStage("dtb_rqt", "dtb_stg", JDBC.name(), ids, dateLimit);
    }

    @Transactional(rollbackFor = Throwable.class)
    public int purgeDtbRequestStage(){
        return purgeRequestStage("dtb_rqt", "dtb_stg", JDBC.name());
    }

    public int purgeLocalRequest(String ids, Timestamp dateLimit){
        return purgeRequest("lcl_rqt", ids, dateLimit, true);
    }

    public int purgeLocalRequest(){
        return purgeRequest("lcl_rqt");
    }

    public int purgeLogEntry(String ids, Timestamp dateLimit){
        return purgeRequest("log_ent", ids, dateLimit, false);
    }

    public int purgeLogEntry(){
        return purgeRequest("log_ent");
    }

    // Public helpers to vacuum each table individually
    public void vacuumRestSession() {
        vacuum("e_rst_ses");
    }

    public void vacuumMainSession() {
        vacuum("e_main_ses");
    }

    public void vacuumRestRequest() {
        vacuum("e_rst_rqt");
    }

    public void vacuumDtbRequest() {
        vacuum("e_dtb_rqt");
    }

    public void vacuumFtpRequest() {
        vacuum("e_ftp_rqt");
    }

    public void vacuumSmtpRequest() {
        vacuum("e_smtp_rqt");
    }

    public void vacuumLdapRequest() {
        vacuum("e_ldap_rqt");
    }

    public void vacuumLocalRequest() {
        vacuum("e_lcl_rqt");
    }

    public void vacuumRestSessionStage() {
        vacuum("e_rst_ses_stg");
    }

    public void vacuumRestRequestStage() {
        vacuum("e_rst_rqt_stg");
    }

    public void vacuumSmtpStage() {
        vacuum("e_smtp_stg");
    }

    public void vacuumFtpStage() {
        vacuum("e_ftp_stg");
    }

    public void vacuumLdapStage() {
        vacuum("e_ldap_stg");
    }

    public void vacuumDtbStage() {
        vacuum("e_dtb_stg");
    }

    public void vacuumInstanceTrace() {
        vacuum("e_ins_trc");
    }

    public void vacuumResourceUsage() {
        vacuum("e_rsc_usg");
    }

    private int purgeRequest(String tableSuffix, String ids, Timestamp dateLimit, boolean withEnd) {
        return template.update("DELETE FROM e_" + tableSuffix +
                " WHERE dh_str < '" + dateLimit + "'" +
                (withEnd ? " AND dh_end < '" + dateLimit + "'" : "") +
                " AND cd_ins IN (" + ids + ");");
    }

    private int purgeRequest(String tableSuffix) {
        return template.update("DELETE FROM e_" + tableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_env_ins WHERE id_ins = cd_ins);");
    }


    private int purgeRequestStage(String tableSuffix, String stageTableSuffix, String type) {
        var queryStage = "DELETE FROM e_" + stageTableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_" + tableSuffix + " = cd_" + tableSuffix + ");";
        var queryException = "DELETE FROM e_exc_inf" +
                " WHERE va_typ = '" + type + "'" +
                " AND NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_" + tableSuffix + " = cd_rqt);";
        return stream(template.batchUpdate(queryStage, queryException)).sum();
    }

    private int purgeSessionStage(String tableSuffix, String stageTableSuffix) {
        return template.update("DELETE FROM e_" + stageTableSuffix +
                " WHERE NOT EXISTS (SELECT 1 FROM e_" + tableSuffix + " WHERE id_ses = cd_prn_ses);");
    }

    private int purgeRequestStage(String tableSuffix, String stageTableSuffix, String type, String ids, Timestamp dateLimit) {
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

    private void vacuum(String tableSuffix) {
        try {
            template.execute("VACUUM ANALYZE " + tableSuffix + ";");
        }
        catch (Exception e) {
            log.error("Error during vacuum analyze on table {}: {}", tableSuffix, e.getMessage());
        }
    }
}
