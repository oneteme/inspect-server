package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.RequestCompletableType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.function.ToLongFunction;

import static java.sql.Types.*;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.server.Utils.*;
import static org.usf.inspect.server.model.RequestCompletableType.*;


/**
 * Using Types.OTHER with JSON serialization to ensure portability:
 * - In PostgreSQL: Types.OTHER is interpreted as native JSONB type
 * - In H2: Types.OTHER is treated as VARCHAR without JSON parsing attempts
 * Prior serialization with writeValueAsString prevents conversion errors
 * between the two DBMSs, making the code compatible with both environments.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TraceDao {
	
    private final JdbcTemplate template;
    private final ObjectMapper mapper;

    private static final int BATCH_SIZE = 1_000;

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
insert into e_env_ins(id_ins,va_typ,dh_str,va_app,va_vrs,va_adr,va_env,va_os,va_re,va_usr,va_clr,va_brch,va_hsh,va_cnf,va_rsr,va_add_prp)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", ps -> {
            ps.setString(1, instance.getId());
            ps.setString(2, ofNullable(instance.getType()).map(InstanceType::name).orElse(null));
            ps.setTimestamp(3, fromNullableInstant(instance.getInstant()));
            ps.setString(4, instance.getName());
            ps.setString(5, instance.getVersion());
            ps.setString(6, instance.getAddress());
            ps.setString(7, instance.getEnv());
            ps.setString(8, instance.getOs());
            ps.setString(9, instance.getRe());
            ps.setString(10, instance.getUser());
            ps.setString(11, instance.getCollector());
            ps.setString(12, instance.getBranch());
            ps.setString(13, instance.getHash());
            try {
                ps.setObject(14, toJsonOrNull(instance.getConfiguration()), OTHER);
                ps.setObject(15, toJsonOrNull(instance.getResource()), OTHER);
                ps.setObject(16, toJsonOrNull(instance.getAdditionalProperties()), OTHER);
            } catch (JsonProcessingException e) {
                throw new SQLException("parsing parameter", e);
            }
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> instances){
        executeBatch("update e_env_ins set dh_end = ? where id_ins = ?::uuid", instances.iterator(), (ps, ins) -> {
            ps.setTimestamp(1, fromNullableInstant(ins.getEnd()));
            ps.setString(2, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveInstanceTraces(List<InstanceTrace> instanceTraces) {
        executeBatch("insert into e_ins_trc (va_pnd, va_atp, va_trc_cnt, dh_str, va_fln, cd_ins) values (?, ?, ?, ?, ?, ?::uuid)", 
        		instanceTraces.iterator(), (ps, trc) -> {
		            ps.setObject(1, trc.getPending(), INTEGER);
		            ps.setObject(2, trc.getAttempts(), INTEGER);
		            ps.setInt(3, trc.getTraceCount());
		            ps.setTimestamp(4, fromNullableInstant(trc.getInstant()));
		            ps.setString(5, trc.getFileName());
		            ps.setString(6, trc.getInstanceId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLogEntries(List<LogEntry> logEntries) {
        executeBatch("insert into e_log_ent(va_lvl,va_msg,va_stk,dh_str,cd_prn_ses,cd_ins) values (?,?,?,?,?::uuid,?::uuid)", 
        		logEntries.iterator(), (ps, o)-> {
		            ps.setString(1, String.valueOf(o.getLevel()));
		            ps.setString(2, o.getMessage());
		            ps.setObject(3, toJsonOrNull(o.getStackRows()), OTHER);
		            ps.setTimestamp(4, fromNullableInstant(o.getInstant()));
		            ps.setString(5, o.getSessionId());
		            ps.setString(6, o.getInstanceId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,cd_ins) values (?,?,?,?,?::uuid)", usages.iterator(), (ps, o)-> {
            ps.setTimestamp(1, fromNullableInstant(o.getInstant()));
            ps.setInt(2, o.getUsedHeap());
            ps.setInt(3, o.getCommitedHeap());
            ps.setInt(4, o.getUsedDiskSpace());
            ps.setString(5, o.getInstanceId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessions(List<RestSession> sessions) {
        completableProcess(REST_SESSION, sessions, toUpdate ->
                executeBatch("""
update e_rst_ses set va_mth = ?, va_pcl = ?, va_hst = ?, cd_prt = ?, va_pth = ?, va_qry = ?, va_cnt_typ = ?, va_ath_sch = ?, cd_stt = ?, va_i_sze = ?, va_o_sze = ?, va_i_cnt_enc = ?, va_o_cnt_enc = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_err_typ = ?, va_err_msg = ?, va_stk = ?, va_nam = ?, va_usr = ?, va_usr_agt = ?, va_cch_ctr = ?, va_msk = ?
where id_ses = ?::uuid""", toUpdate, (ps, ses) -> {
            restSessionSetter(ps, ses, mapper);
        }), toInsert ->
                executeBatch("""
insert into e_rst_ses(va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_cnt_typ,va_ath_sch,cd_stt,va_i_sze,va_o_sze,va_i_cnt_enc,va_o_cnt_enc,dh_str,dh_end,va_thr,va_err_typ,va_err_msg,va_stk,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_msk,id_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, ses) -> {
            restSessionSetter(ps, ses, mapper);
            ps.setString(26, ses.getInstanceId());
        }));
    }

    static void restSessionSetter(PreparedStatement ps, RestSession ses, ObjectMapper mapper) throws SQLException, JsonProcessingException {
        var exp = ses.getException();
        ps.setString(1, ses.getMethod());
        ps.setString(2, ses.getProtocol());
        ps.setString(3, ses.getHost());
        ps.setInt(4, ses.getPort());
        ps.setString(5, ses.getPath());
        ps.setString(6, ses.getQuery());
        ps.setString(7, contentTypeExtract(ses.getContentType()));
        ps.setString(8, ses.getAuthScheme());
        ps.setInt(9, ses.getStatus());
        ps.setLong(10, ses.getInDataSize());
        ps.setLong(11, ses.getOutDataSize());
        ps.setString(12, ses.getInContentEncoding());
        ps.setString(13, ses.getOutContentEncoding());
        ps.setTimestamp(14, fromNullableInstant(ses.getStart()));
        ps.setTimestamp(15, fromNullableInstant(ses.getEnd()));
        ps.setString(16, ses.getThreadName());
        ps.setString(17, nonNull(exp) ? exp.getType() : null);
        ps.setString(18, nonNull(exp) ? exp.getMessage() : null);
        ps.setObject(19, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
        ps.setString(20, ses.getName());
        ps.setString(21, ses.getUser());
        ps.setString(22, userAgentExtract(ses.getUserAgent()));
        ps.setString(23, ses.getCacheControl());
        ps.setInt(24, ses.getRequestsMask());
        ps.setString(25, ses.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessions(List<MainSession> sessions) {
        completableProcess(MAIN_SESSION, sessions, toUpdate ->
                executeBatch("""
update e_main_ses set va_nam = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_typ = ?, va_lct = ?, va_thr = ?, va_err_typ = ?, va_err_msg = ?, va_stk = ?, va_msk = ?
where id_ses = ?::uuid""", toUpdate, (ps, ses) -> {
                    mainSessionSetter(ps, ses, mapper);
                }), toInsert ->
                executeBatch("""
insert into e_main_ses(va_nam,va_usr,dh_str,dh_end,va_typ,va_lct,va_thr,va_err_typ,va_err_msg,va_stk,va_msk,id_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?::json,?,?::uuid,?::uuid)""", toInsert, (ps, ses) -> {
            mainSessionSetter(ps, ses, mapper);
            ps.setString(13, ses.getInstanceId());
        }));
    }

    static void mainSessionSetter(PreparedStatement ps, MainSession ses, ObjectMapper mapper) throws SQLException, JsonProcessingException {
        var exp = ses.getException();
        ps.setString(1, ses.getName());
        ps.setString(2, ses.getUser());
        ps.setTimestamp(3, fromNullableInstant(ses.getStart()));
        ps.setTimestamp(4, fromNullableInstant(ses.getEnd()));
        ps.setString(5, valueOfNullable(ses.getType()));
        ps.setString(6, ses.getLocation());
        ps.setString(7, ses.getThreadName());
        ps.setString(8, nonNull(exp) ? exp.getType() : null);
        ps.setString(9, nonNull(exp) ? exp.getMessage() : null);
        ps.setObject(10, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
        ps.setInt(11, ses.getRequestsMask());
        ps.setString(12, ses.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequests(List<RestRequest> requests) {
        completableProcess(REST_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_rst_rqt set va_mth = ?, va_pcl = ?, va_hst = ?, cd_prt = ?, va_pth = ?, va_qry = ?, va_cnt_typ = ?, va_ath_sch = ?, cd_stt = ?, va_i_sze = ?, va_o_sze = ?, va_i_cnt_enc = ?, va_o_cnt_enc = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_bdy_cnt = ?, va_lnk = ?
where id_rst_rqt = ?::uuid""", toUpdate, TraceDao::restRequestSetter), toInsert ->
                executeBatch("""
insert into e_rst_rqt(va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_cnt_typ,va_ath_sch,cd_stt,va_i_sze,va_o_sze,va_i_cnt_enc,va_o_cnt_enc,dh_str,dh_end,va_thr,va_bdy_cnt,va_lnk,id_rst_rqt,cd_prn_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
            restRequestSetter(ps, req);
            ps.setString(20, req.getSessionId());
            ps.setString(21, req.getInstanceId());
        }));
    }

    static void restRequestSetter(PreparedStatement ps, RestRequest req) throws SQLException {
        ps.setString(1, req.getMethod());
        ps.setString(2, req.getProtocol());
        ps.setString(3, req.getHost());
        ps.setInt(4, req.getPort());
        ps.setString(5, req.getPath());
        ps.setString(6, req.getQuery());
        ps.setString(7, contentTypeExtract(req.getContentType()));
        ps.setString(8, req.getAuthScheme());
        ps.setInt(9, req.getStatus());
        ps.setLong(10, req.getInDataSize());
        ps.setLong(11, req.getOutDataSize());
        ps.setString(12, req.getInContentEncoding());
        ps.setString(13, req.getOutContentEncoding());
        ps.setTimestamp(14, fromNullableInstant(req.getStart()));
        ps.setTimestamp(15, fromNullableInstant(req.getEnd()));
        ps.setString(16, req.getThreadName());
        ps.setString(17, req.getBodyContent());
        ps.setBoolean(18, req.isLinked());
        ps.setString(19, req.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLocalRequests(List<LocalRequest> requests){
        completableProcess(LOCAL_REQUEST, requests, 
        		toUpdate -> executeBatch("""
UPDATE e_lcl_rqt SET va_nam=?,va_lct=?,dh_str=?,dh_end=?,va_usr=?,va_thr=?,va_fail=?,va_typ=? 
WHERE id_lcl_rqt = ?::uuid""", toUpdate, TraceDao::localRequestSetter), 
                toInsert -> executeBatch("""
INSERT INTO e_lcl_rqt(va_nam,va_lct,dh_str,dh_end,va_usr,va_thr,va_fail,va_typ,id_lcl_rqt,cd_prn_ses,cd_ins)
(?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
			localRequestSetter(ps, req);
            ps.setString(10, req.getSessionId());
            ps.setString(11, req.getInstanceId());
        }));
        saveLocalRequestExceptions(requests);
    }
    
    static void localRequestSetter(PreparedStatement ps, LocalRequest req) throws SQLException {
        ps.setString(1,req.getName());
        ps.setString(2,req.getLocation());
        ps.setTimestamp(3,fromNullableInstant(req.getStart()));
        ps.setTimestamp(4,fromNullableInstant(req.getEnd()));
        ps.setString(5,req.getUser());
        ps.setString(6,req.getThreadName());
        ps.setBoolean(7, nonNull(req.getException()));
        ps.setString(8, req.getType());
        ps.setString(9, req.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequests(List<MailRequest> requests) {
        completableProcess(SMTP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_smtp_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_cmd = ?, va_fail = ?
where id_smtp_rqt = ?::uuid""", toUpdate, TraceDao::mailRequestSetter), toInsert ->
                executeBatch("""
insert into e_smtp_rqt(va_hst,cd_prt,va_pcl,va_usr,dh_str,dh_end,va_thr,va_cmd,va_fail,id_smtp_rqt,cd_prn_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
            mailRequestSetter(ps, req);
            ps.setString(11, req.getSessionId());
            ps.setString(12, req.getInstanceId());
        }));
    }

    static void mailRequestSetter(PreparedStatement ps, MailRequest req) throws SQLException {
        ps.setString(1, req.getHost());
        ps.setInt(2, req.getPort());
        ps.setString(3, req.getProtocol());
        ps.setString(4, req.getUser());
        ps.setTimestamp(5, fromNullableInstant(req.getStart()));
        ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
        ps.setString(7, req.getThreadName());
        ps.setString(8, req.getCommand());
        ps.setBoolean(9, req.isFailed());
        ps.setString(10, req.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequests(List<FtpRequest> requests){
        completableProcess(FTP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_ftp_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_srv_vrs = ?, va_clt_vrs = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_cmd = ?, va_fail = ?
where id_ftp_rqt = ?::uuid""", toUpdate, TraceDao::ftpRequestSetter), toInsert ->
                executeBatch("""
insert into e_ftp_rqt(va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,dh_str,dh_end,va_thr,va_cmd,va_fail,id_ftp_rqt,cd_prn_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                    ftpRequestSetter(ps, req);
                    ps.setString(13, req.getSessionId());
                    ps.setString(14, req.getInstanceId());
                }));
    }

    static void ftpRequestSetter(PreparedStatement ps, FtpRequest req) throws SQLException {
        ps.setString(1, req.getHost());
        ps.setInt(2, req.getPort());
        ps.setString(3, req.getProtocol());
        ps.setString(4, req.getServerVersion());
        ps.setString(5, req.getClientVersion());
        ps.setString(6, req.getUser());
        ps.setTimestamp(7, fromNullableInstant(req.getStart()));
        ps.setTimestamp(8, fromNullableInstant(req.getEnd()));
        ps.setString(9, req.getThreadName());
        ps.setString(10, req.getCommand());
        ps.setBoolean(11, req.isFailed());
        ps.setString(12, req.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequests(List<DirectoryRequest> requests) {
        completableProcess(LDAP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_ldap_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_cmd = ?, va_fail = ?
where id_ldap_rqt = ?::uuid""", toUpdate, TraceDao::ldapRequestSetter), toInsert ->
                executeBatch("""
insert into e_ldap_rqt(va_hst,cd_prt,va_pcl,va_usr,dh_str,dh_end,va_thr,va_cmd,va_fail,id_ldap_rqt,cd_prn_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                ldapRequestSetter(ps, req);
                ps.setString(11, req.getSessionId());
                ps.setString(12, req.getInstanceId());
            }));
    }

    static void ldapRequestSetter(PreparedStatement ps, DirectoryRequest req) throws SQLException {
        ps.setString(1, req.getHost());
        ps.setInt(2, req.getPort());
        ps.setString(3, req.getProtocol());
        ps.setString(4, req.getUser());
        ps.setTimestamp(5, fromNullableInstant(req.getStart()));
        ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
        ps.setString(7, req.getThreadName());
        ps.setString(8, req.getCommand());
        ps.setBoolean(9, req.isFailed());
        ps.setString(10, req.getId());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequests(List<DatabaseRequest> requests) {
        completableProcess(JDBC_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_dtb_rqt set va_hst = ?, cd_prt = ?, va_she = ?, va_nam = ?, va_sha = ?, dh_str = ?, dh_end = ?, va_usr = ?, va_thr = ?, va_drv = ?, va_prd_nam = ?, va_prd_vrs = ?, va_cmd = ?, va_fail = ?
where id_dtb_rqt = ?::uuid""", toUpdate, TraceDao::databaseRequestSetter), toInsert ->
                executeBatch("""
insert into e_dtb_rqt(va_hst,cd_prt,va_she,va_nam,va_sha,dh_str,dh_end,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,va_cmd,va_fail,id_dtb_rqt,cd_prn_ses,cd_ins)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                databaseRequestSetter(ps, req);
                ps.setString(16, req.getSessionId());
                ps.setString(17, req.getInstanceId());
            }));
    }

    static void databaseRequestSetter(PreparedStatement ps, DatabaseRequest req) throws SQLException {
        ps.setString(1, req.getHost());
        ps.setInt(2, req.getPort());
        ps.setString(3, req.getScheme());
        ps.setString(4, req.getName());
        ps.setString(5, req.getSchema());
        ps.setTimestamp(6, fromNullableInstant(req.getStart()));
        ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
        ps.setString(8, req.getUser());
        ps.setString(9, req.getThreadName());
        ps.setString(10, req.getDriverVersion());
        ps.setString(11, req.getProductName());
        ps.setString(12, req.getProductVersion());
        ps.setString(13, req.getCommand());
        ps.setBoolean(14, req.isFailed());
        ps.setString(15, req.getId());
    }

    private <T extends CompletableMetric> void completableProcess(
            RequestCompletableType type,
            List<T> items,
            ToLongFunction<Iterator<T>> updateBatchExecutor,
            ToLongFunction<Iterator<T>> insertBatchExecutor) {
        var completableMetrics = template.queryForList("SELECT id_cmp_mtc FROM e_cmp_mtc WHERE cd_typ=" + type.getValue(), String.class);
        var toUpdate = items.stream().filter(s -> completableMetrics.contains(s.getId())).toList();

        if(!toUpdate.isEmpty()) {
            updateBatchExecutor.applyAsLong(toUpdate.iterator()); //already logged
            var completedMetrics = toUpdate.stream()
                    .filter(s-> nonNull(s.getEnd()))
                    .map(CompletableMetric::getId)
                    .toArray(); //Insertion des sessions uncompleted
            if(completedMetrics.length > 0) {
                template.update(new StringBuilder("DELETE FROM e_cmp_mtc WHERE id_cmp_mtc IN(?::uuid")
                		.append(",?::uuid".repeat(completedMetrics.length - 1))
                		.append(" AND cd_typ=").append(type.getValue()).toString(), completedMetrics);
            }
        }
        //savePoint !!
        var toInsert = items.stream().filter(s -> !completableMetrics.contains(s.getId())).toList();
        if(!toInsert.isEmpty()) {
            insertBatchExecutor.applyAsLong(toInsert.iterator());
            var uncompletedMetrics = toInsert.stream()
                    .filter(s-> isNull(s.getEnd()))
                    .map(CompletableMetric::getId)
                    .toList(); //Insertion des sessions uncompleted
            if(!uncompletedMetrics.isEmpty()) {
                executeBatch(new StringBuilder("INSERT INTO e_cmp_mtc(id_cmp_mtc,cd_typ) VALUES(?::uuid,")
                		.append(type.getValue()).append(')').toString(), uncompletedMetrics.iterator(), 
                		(ps, id) -> ps.setString(1, id));
            }
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        executeBatch("insert into e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setInt(4, stg.getOrder());
            ps.setString(5, stg.getRequestId());
        });
        saveStageExceptions(stages, REST);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpSessionStages(List<HttpSessionStage> stages) {
        executeBatch("insert into e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setInt(4, stg.getOrder());
            ps.setString(5, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) values(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setInt(5, stg.getOrder());
            ps.setString(6, stg.getRequestId());
        });
        saveMailRequestMails(stages);
        saveStageExceptions(stages, SMTP);
    }

    private void saveMailRequestMails(List<MailRequestStage> mails) {
        executeBatch("insert into e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) values(?,?,?,?,?,?,?::uuid)", 
        		mails.stream().filter(m -> nonNull(m.getMail())).iterator(), (ps, stg)-> {
		            ps.setString(1, stg.getMail().getSubject());
		            ps.setString(2, stg.getMail().getContentType());
		            if(nonNull(stg.getMail())) { //
		            	var mail = stg.getMail();
		                ps.setString(3, joinValuesOrNull(mail.getFrom()));
		                ps.setString(4, joinValuesOrNull(mail.getRecipients()));
		                ps.setString(5, joinValuesOrNull(mail.getReplyTo()));
		            }
		            else {
		            	ps.setNull(3, VARCHAR);
		            	ps.setNull(4, VARCHAR);
		            	ps.setNull(5, VARCHAR);
		            }
		            ps.setInt(6,stg.getMail().getSize());
		            ps.setString(7, stg.getRequestId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ftp_rqt) values(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setString(5, joinValuesOrNull(stg.getArgs()));
            ps.setInt(6, stg.getOrder());
            ps.setString(7, stg.getRequestId());
        });
        saveStageExceptions(stages, FTP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestStages(List<DirectoryRequestStage> stages) {
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ldap_rqt) values(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setString(5, joinValuesOrNull(stg.getArgs()));
            ps.setInt(6, stg.getOrder());
            ps.setString(7, stg.getRequestId());
        });
        saveStageExceptions(stages, LDAP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cnt,va_cmd,va_arg,cd_ord,cd_dtb_rqt) values(?,?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, valueOfNullableArray(stg.getCount()));
            ps.setString(5, stg.getCommand());
            ps.setString(6, joinValuesOrNull(stg.getArgs()));
            ps.setInt(7, stg.getOrder());
            ps.setString(8, stg.getRequestId());
        });
        saveStageExceptions(stages, JDBC);
    }

    private void saveStageExceptions(List<? extends AbstractStage> stages, RequestMask mask) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException()))
                .iterator();
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", exceptions, (ps, exp) -> {
            ps.setString(1, mask.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, toJsonOrNull(exp.getException().getStackTraceRows()), OTHER);
            ps.setInt(5, exp.getOrder());
            ps.setString(6, exp.getRequestId());
        });
    }

    private void saveLocalRequestExceptions(List<LocalRequest> stages) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException()))
                .iterator();
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", exceptions, (ps, exp) -> {
            ps.setString(1, LOCAL.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, toJsonOrNull(exp.getException().getStackTraceRows()), OTHER);
            ps.setInt(5, 0);
            ps.setString(6, exp.getId());
        });
    }

    private <T> Long executeBatch(String sql, Iterator<T> it, PreparedStatementSetter<T> pss) {
        return it.hasNext() ? template.execute(sql, (PreparedStatement ps) -> {
        	long idx = 0;
            long rows = 0;
            try {
                while (it.hasNext()) {
                    pss.setValues(ps, it.next());
                    ps.addBatch();
                    if (++idx % BATCH_SIZE == 0) {
                        rows += stream(ps.executeBatch()).sum();
                    }
                }
            }
            catch (JsonProcessingException e) {
            	throw new SQLException("parsing parameter, row="+idx, e);
            }
            if (idx % BATCH_SIZE > 0) {
                rows += stream(ps.executeBatch()).sum();
            }
            log.debug("{}/{} rows was updated", idx, rows);
            return rows;
        }) : 0L;
    }

    String toJsonOrNull(Object o) throws JsonProcessingException {
    	return nonNull(o) ? mapper.writeValueAsString(o) : null;
    }
    
    static String joinValuesOrNull(String... args) {
    	return nonNull(args) ? String.join(", ", args) : null;
    }
    
    @FunctionalInterface
    interface PreparedStatementSetter<T> {
    	
    	void setValues(PreparedStatement ps, T argument) throws SQLException, JsonProcessingException;
    }
}
